"""Contrato de carteira anual: validação, estatística e séries, sem acesso ao banco."""
import math
import re
import unicodedata
from collections import Counter
from datetime import date, datetime
from estatistica import resumo, testes_estatisticos

FIELDS = ('codigo_cliente','nome_cliente','consultor','segmento','nivel_cliente','faturamento_anual','servicos_contratados','data_contratacao')
SEGMENTS = {'ind':'Indústria','industria':'Indústria','comercio':'Comércio','servico':'Serviços','servicos':'Serviços','saude':'Saúde','educacao':'Educação','tecnologia':'Tecnologia'}
ALIASES = {'codigo':'codigo_cliente','nome':'nome_cliente','cliente':'nome_cliente','nivel':'nivel_cliente'}

def key(value):
    return re.sub(r'[^a-z0-9]+', '_', unicodedata.normalize('NFD', str(value)).encode('ascii','ignore').decode().lower()).strip('_')

def text(value):
    return ' '.join(str(value if value is not None else '').split())

def title(value):
    return ' '.join(word.lower() if i and word.lower() in ('de','da','do','dos','das','e') else word.capitalize() for i,word in enumerate(text(value).split()))

def money(value):
    from decimal import Decimal, InvalidOperation
    value = text(value).replace('R$', '').replace(' ', '')
    if ',' in value and '.' in value:
        value = value.replace('.', '').replace(',', '.') if value.rfind(',') > value.rfind('.') else value.replace(',', '')
    elif ',' in value: value = value.replace(',', '.')
    elif re.fullmatch(r'\d{1,3}(\.\d{3})+', value): value = value.replace('.', '')
    try:
        number = Decimal(value)
        return float(number) if number.is_finite() and 0 <= number < Decimal('1e12') and number == number.quantize(Decimal('.01')) else None
    except InvalidOperation: return None

def iso(value):
    if isinstance(value, (date, datetime)): return value.isoformat()[:10]
    for fmt in ('%Y-%m-%d','%d/%m/%Y','%Y-%m-%d %H:%M:%S'):
        try:
            result = datetime.strptime(text(value), fmt).date()
            return result.isoformat() if result.year >= 1990 else None
        except ValueError: pass
    return None

def normalize(records):
    valid, invalid, warnings, seen = [], [], [], set()
    for index, original in enumerate(records):
        row = {ALIASES.get(key(k), key(k)): text(v) for k,v in original.items()}
        errors = [f'{f}: obrigatório' for f in FIELDS if not row.get(f)]
        row['linha_origem'] = original.get('linha_origem', index+2)
        for field,limit in {'codigo_cliente':60,'nome_cliente':180,'consultor':120,'segmento':80,'servicos_contratados':400,'cidade':120,'uf':2}.items():
            if len(row.get(field,'')) > limit: errors.append(field+': tamanho excedido')
        row['codigo_cliente'] = row.get('codigo_cliente','').upper()
        if not re.fullmatch(r'[A-Z0-9_-]{1,60}', row['codigo_cliente']): errors.append('codigo_cliente: formato inválido')
        if row['codigo_cliente'] in seen: errors.append('codigo_cliente: duplicado')
        seen.add(row['codigo_cliente'])
        segment = SEGMENTS.get(key(row.get('segmento','')))
        if not segment: errors.append('segmento: não reconhecido')
        row['segmento'] = segment or row.get('segmento','')
        row['nivel_cliente'] = row.get('nivel_cliente','').upper()
        if row['nivel_cliente'] not in ('A','B','C'): errors.append('nivel_cliente: inválido')
        row['faturamento_anual'] = money(row.get('faturamento_anual',''))
        if row['faturamento_anual'] is None: errors.append('faturamento_anual: inválido')
        row['data_contratacao'] = iso(row.get('data_contratacao',''))
        if not row['data_contratacao']: errors.append('data_contratacao: inválida')
        row['consultor'] = title(row.get('consultor',''))
        row['cidade'] = title(row.get('cidade',''))
        row['uf'] = row.get('uf','').upper()
        if row['uf'] and not re.fullmatch(r'[A-Z]{2}', row['uf']): errors.append('uf: inválida')
        services = list(dict.fromkeys(title(s) for s in row.get('servicos_contratados','').split(';') if text(s)))
        if not services: errors.append('servicos_contratados: obrigatório')
        row['servicos_contratados'] = ';'.join(services)
        row['servicos'] = services
        if errors: invalid.append({**row,'erros':errors,'original':original})
        else:
            changes = [f for f in FIELDS+('cidade','uf') if text(original.get(f,'')) != str(row.get(f,''))]
            if changes or row['faturamento_anual'] == 0 or row['data_contratacao'] > date.today().isoformat(): warnings.append({'linha':row['linha_origem'],'campos':changes})
            valid.append(row)
    return valid, invalid, warnings

def processar_anual(records, headers):
    fields = {ALIASES.get(key(h),key(h)) for h in headers}
    missing = set(FIELDS)-fields
    if missing: raise ValueError('Colunas obrigatórias ausentes: '+', '.join(sorted(missing)))
    valid, invalid, warnings = normalize(records)
    values = [r['faturamento_anual'] for r in valid]
    overall = resumo(values)
    total = overall.get('total',0)
    def groups(field):
        result=[]
        for name in sorted({r.get(field) or 'Não informado' for r in valid}):
            sample=[r['faturamento_anual'] for r in valid if (r.get(field) or 'Não informado')==name]
            result.append({'name':name,**resumo(sample),'percentage':len(sample)/len(valid)*100,'revenuePercentage':sum(sample)/total*100 if total else 0})
        return result
    bysegment, bylevel, byconsultant = groups('segmento'),groups('nivel_cliente'),groups('consultor')
    service_counts=Counter(s for r in valid for s in r['servicos'])
    services=[{'name':s,'count':c,'percentage':c/len(valid)*100} for s,c in service_counts.most_common()]
    monthly=Counter(r['data_contratacao'][:7] for r in valid)
    timeline=[]
    if monthly:
        y,m=map(int,min(monthly).split('-')); last=max(monthly); accumulated=0
        while f'{y:04}-{m:02}' <= last:
            month=f'{y:04}-{m:02}'; count=monthly[month]; accumulated+=count
            timeline.append({'month':month,'count':count,'accumulated':accumulated,'revenue':sum(r['faturamento_anual'] for r in valid if r['data_contratacao'].startswith(month))})
            y,m=(y+1,1) if m==12 else (y,m+1)
    correlation={'available':False,'reason':'Exige três clientes e variação em serviços e faturamento.','n':len(valid),'pearson':None,'spearman':None}
    if len(valid)>=3 and len({len(r['servicos']) for r in valid})>1 and len(set(values))>1:
        from scipy.stats import pearsonr,spearmanr
        x=[len(r['servicos']) for r in valid]
        p,sp=pearsonr(x,values),spearmanr(x,values)
        correlation={'available':True,'n':len(valid),'pearson':float(p.statistic),'pValue':float(p.pvalue),'spearman':float(sp.statistic),'spearmanPValue':float(sp.pvalue),'interpretation':'Associação entre quantidade de serviços e faturamento; não demonstra causalidade.'}
    # O módulo de testes recebe uma projeção numérica. O período permanece anual na saída.
    projected=[{**r,'nivel':r['nivel_cliente'],'faturamento_mensal':r['faturamento_anual']} for r in valid]
    stats={'overall':overall,'bySegment':bysegment,'byLevel':bylevel,'byConsultant':byconsultant}
    from graficos import series_graficos
    plotrows=[{**r,'codigo_cliente':r['codigo_cliente'],'faturamento_mensal':r['faturamento_anual']} for r in valid]
    charts=series_graficos(plotrows,stats)
    charts['contractsByMonth']=timeline
    charts['revenueServices']=[{'x':len(r['servicos']),'y':r['faturamento_anual']} for r in valid]
    kpis={'totalClients':len(valid),'totalRevenue':total,'averageTicket':overall.get('mean'),'contractCount':sum(service_counts.values()),'activeContracts':None,'cancellationRate':None,'averageServices':sum(service_counts.values())/len(valid) if valid else None,'coefficientOfVariation':overall.get('std')/overall['mean'] if overall.get('std') is not None and overall.get('mean') else None}
    top=sorted(valid,key=lambda r:(-r['faturamento_anual'],r['codigo_cliente']))[:5]
    insights=[]
    if valid:
        largest=max(bysegment,key=lambda g:g['count']); leader=max(byconsultant,key=lambda g:g['total'])
        insights=[f"O segmento {largest['name']} reúne {largest['count']} clientes.",f"O faturamento anual médio é R$ {overall['mean']:,.2f}; a mediana é R$ {overall['median']:,.2f}.",f"{leader['name']} possui o maior faturamento anual sob gestão."]
        if services: insights.append(f"{services[0]['name']} é o serviço mais contratado ({services[0]['count']} clientes).")
        if correlation['available']: insights.append(f"Correlação de Pearson: {correlation['pearson']:.3f}. Associação não implica causalidade.")
    return {'schema':'carteira-anual-v1','revenuePeriod':'anual','inputRows':len(records),'validRows':len(valid),'invalidRows':len(invalid),'warningRows':len(warnings),'warnings':warnings,'validRecords':valid,'invalidRecords':invalid,'statistics':stats,'statisticalTests':testes_estatisticos(projected),'indicators':kpis,'insights':insights,'charts':charts,'services':services,'topClients':top,'correlation':correlation,'byCity':groups('cidade'),'byState':groups('uf')}
