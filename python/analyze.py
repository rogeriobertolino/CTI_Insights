import json
import re
import sys
import unicodedata
from pathlib import Path
import pandas as pd

def slug(value):
    text=unicodedata.normalize("NFD",str(value)).encode("ascii","ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+","_",text).strip("_")

def find(columns,aliases):
    normalized=[slug(c) for c in columns]
    # A generic alias such as "cliente" must not shadow an exact name column.
    for alias in aliases:
        if alias in normalized:return normalized.index(alias)
    for alias in aliases:
        for index,column in enumerate(normalized):
            if alias in column:return index
    return -1

def distribution(frame,index,split=False):
    if index<0:return []
    values=frame.iloc[:,index].fillna("").astype(str)
    if split: values=values.str.split(r"[,;|/]").explode().str.strip()
    counts=values[values.str.strip()!=""].value_counts().head(20)
    return [{"name":str(name),"value":int(value)} for name,value in counts.items()]

def normalized_segment(value):
    key=slug(value)
    return {"ind":"Indústria","industria":"Indústria","industrial":"Indústria",
            "com":"Comércio","comercio":"Comércio","serv":"Serviços",
            "servico":"Serviços"}.get(key,str(value).strip())

def normalized_level(value):
    value=str(value).strip().upper()
    return value if value in {"A","B","C"} else value

def parse_money(value):
    text=str(value).strip().replace("R$","").replace(" ","")
    if not text:return None
    if "," in text:
        text=text.replace(".","").replace(",", ".")
    try:return float(text)
    except ValueError:return None

def descriptive_statistics(frame,index):
    if index<0:return {}
    values=frame.iloc[:,index].map(parse_money).dropna()
    if values.empty:return {}
    return {"count":int(values.count()),"mean":round(float(values.mean()),2),
            "median":round(float(values.median()),2),"std":round(float(values.std(ddof=1) or 0),2),
            "min":round(float(values.min()),2),"max":round(float(values.max()),2),
            "q1":round(float(values.quantile(.25)),2),"q3":round(float(values.quantile(.75)),2)}

def main(path):
    frame=pd.read_excel(path,sheet_name=0,dtype=str,keep_default_na=False).fillna("")
    frame=frame.map(lambda value:str(value).strip())
    headers=[]
    for index,column in enumerate(frame.columns):
        base=str(column).strip() or f"Coluna {index+1}"
        name=base
        suffix=2
        while name in headers:
            name=f"{base} ({suffix})"
            suffix+=1
        headers.append(name)
    frame.columns=headers
    indexes={
        "clientCode":find(frame.columns,["cliente_codigo","codigo_cliente","cod_cliente","cnpj"]),
        "clientName":find(frame.columns,["razao_social","nome_cliente","cliente","empresa"]),
        "segment":find(frame.columns,["segmento","setor"]),"status":find(frame.columns,["status","situacao"]),
        "service":find(frame.columns,["servico","produto","solucao"]),"consultant":find(frame.columns,["consultor","responsavel","vendedor"]),
        "level":find(frame.columns,["nivel","classificacao"]),"revenue":find(frame.columns,["faturamento_anual","faturamento","receita"])
    }
    client=indexes["clientCode"] if indexes["clientCode"]>=0 else indexes["clientName"]
    keys=frame.iloc[:,client].map(slug) if client>=0 else pd.Series([],dtype=str)
    non_empty=keys[keys!=""]
    duplicates=int(non_empty.duplicated().sum())
    missing_key=int((keys=="").sum()) if client>=0 else len(frame)
    total_cells=max(frame.shape[0]*frame.shape[1],1)
    empty_cells=int((frame=="").sum().sum())
    completeness=max(0,100-empty_cells/total_cells*100)
    quality=max(0,completeness-duplicates/max(len(frame),1)*20-missing_key/max(len(frame),1)*30)
    status_col=frame.iloc[:,indexes["status"]].astype(str) if indexes["status"]>=0 else None
    active=int(status_col.str.match(r"(?i)^ativo$").sum()) if status_col is not None else None
    if indexes["segment"]>=0:
        frame.iloc[:,indexes["segment"]]=frame.iloc[:,indexes["segment"]].map(normalized_segment)
    if indexes["level"]>=0:
        frame.iloc[:,indexes["level"]]=frame.iloc[:,indexes["level"]].map(normalized_level)
    result={"headers":list(frame.columns),"records":frame.to_dict(orient="records"),"totalRows":len(frame),
        "totalClients":int(non_empty.nunique()+missing_key) if client>=0 else len(frame),"activeClients":active,
        "completeness":round(completeness,2),"quality":round(quality,2),"duplicates":duplicates,"missingKey":missing_key,"emptyCells":empty_cells,
        "segments":distribution(frame,indexes["segment"]),"statuses":distribution(frame,indexes["status"]),
        "services":distribution(frame,indexes["service"],True),"consultants":distribution(frame,indexes["consultant"]),
        "levels":distribution(frame,indexes["level"]),"revenueStatistics":descriptive_statistics(frame,indexes["revenue"]),"columns":indexes}
    print(json.dumps(result,ensure_ascii=False,default=str))

if __name__=="__main__":
    if len(sys.argv)!=2 or not Path(sys.argv[1]).is_file():raise SystemExit("Uso: analyze.py arquivo.xlsx")
    main(sys.argv[1])
