# Compatibilidade com a aula de Python

> Integração Azure concluída em 12/11/2026: migração de compatibilidade aplicada com snapshot prévio; ETL real validado no banco em transação revertida; backend compilado e conectado ao Azure com saúde UP. Os 18 testes Python passaram. O inicializador utiliza as credenciais Azure do `.env` para os dois serviços. Detalhes em `docs/Configuracao_Azure.md`. As limitações de conexão/gravação registradas abaixo pertencem ao diagnóstico anterior.

> Correção posterior do ambiente (12/11/2026): pandas atualizado para 2.3.3 e SciPy para 1.16.3, com versões fixadas em `python/requirements.txt`. Os 10 testes de `test_etl.py` passaram; confirmados 38 registros, 32 clientes válidos, 61 contratos e média 2261250. O Uvicorn foi iniciado em porta temporária, respondeu HTTP 200 com `{"status":"ok"}` e foi encerrado após a verificação. A limitação de carregamento descrita no diagnóstico abaixo foi superada no ambiente corrigido. A gravação real em PostgreSQL/Azure continua fora dessa validação.

> Atualização de implementação em 12/11/2026: foram adaptados indicadores e aliases JSON da aula, qualidade e correções por campo, Spearman, série mensal por clientes com meses vazios e acumulado, gráfico de faturamento por consultor e atualização de lotes pendentes. O POST /testar oferece as estatísticas diretamente e mantém o envelope anterior. A criação opcional de lote agora usa PENDENTE antes da reserva. Docker Compose inclui FastAPI com healthcheck e chave compartilhada; Render declara a API separada. O ambiente `.venv-analytics` foi criado e o inicializador passa a preferi-lo. As observações abaixo documentam o diagnóstico anterior às alterações. As colunas do banco e a criação do lote no Spring foram preservadas intencionalmente.

Validação após adaptação: compilação Vite concluída, 3 testes de frontend passaram (upload da aula e saúde local), análise de sintaxe de 16 arquivos Python concluída e scripts PowerShell analisados sem erro. Foram adicionados testes para meses vazios, contagem de clientes versus contratos, campos JSON e criação sem loteId. A execução Python não concluiu: o diagnóstico com faulthandler localizou a espera no carregamento de uma extensão nativa do pandas, inclusive em uma cópia fora do OneDrive. O ambiente novo e as dependências foram instalados, mas a execução da API e dos cálculos ainda precisa ser confirmada nesta máquina. Docker não estava disponível para validar os contêineres. Não houve implantação nem alteração de registros do banco. Consultas adaptadas estão em `database/conferencia_aula.sql`.

Data da revisão: 12/11/2026.

Referências: `Python.pdf` (24 páginas) e `python-codigos.txt`, fornecidos em Downloads. Revisão dos arquivos atuais do projeto, incluindo alterações ainda não commitadas. O material foi tratado como referência de comparação, sem executar suas instruções de alteração, implantação ou exclusão de dados.

## Parecer

O projeto implementa a arquitetura central da aula, mas não é uma reprodução integral do contrato e do comportamento apresentados. A compatibilidade funcional completa depende de corrigir o ambiente de execução e confirmar a gravação em PostgreSQL. Esta revisão não certifica uma implantação Azure.

## O que está alinhado

- `analytics-python/` fica ao lado de `backend/` e contém api, main, limpeza, analise, insights, banco, config, testar_conexao e amostras.
- O fluxo de upload da aula usa Vue/Pinia, revalidação no Spring, evento após commit e POST HTTP ao Python com linhas válidas.
- Os campos camelCase enviados por `PythonEtlService` são reconhecidos pela transformação Python.
- FastAPI implementa GET `/saude`, POST `/etl`, POST `/testar` e conferência de `X-ETL-Key`.
- Há padronização de segmento, consultor, nível, serviços, valores e datas.
- A análise calcula média, mediana, desvio amostral, quartis, agrupamentos, moda, correlação e insights. Existem ainda testes de hipótese adicionais.
- `banco.py` grava clientes, contratos, estatísticas e status em transação. O Spring lê o JSON para o dashboard Chart.js.

## Diferenças e problemas

### 1. Ambiente Python indicado pelo inicializador está quebrado — prioridade alta

O `PythonExecutable` da configuração local aponta para `analytics312-venv`, cujo executável informa que não encontra o Python 3.12 de origem. Esse ambiente não conseguiu executar os testes. A `.venv` da raiz executa Python 3.13.15 e tem pandas, mas não tem FastAPI. Assim, não foi possível validar a API nesse ambiente alternativo.

Ação recomendada: recriar um ambiente com um interpretador existente, instalar `analytics-python/requirements.txt` e atualizar a configuração utilizada por `scripts/local.ps1`.

### 2. Implantação não inicia a API da aula — prioridade alta

`compose.yml` define frontend, backend e PostgreSQL, mas não um serviço FastAPI nem um `ETL_URL` para ele. O valor padrão do backend é `http://localhost:8000/etl`, que dentro do contêiner aponta para o próprio backend. `render.yaml` também configura o pipeline antigo, sem declarar a API ETL. O Dockerfile de analytics usa como entrada `python/analyze.py`, não uvicorn.

Ação recomendada: declarar/iniciar o serviço FastAPI e configurar o backend para seu endereço, com a mesma chave e o mesmo banco.

### 3. Série mensal mede outra coisa — prioridade média

No TXT, `_por_mes` conta clientes, inclui meses vazios, faturamento e acumulado. No projeto, `analise.py:56` expande serviços e conta contratos por mês, omitindo meses sem registros e os campos faturamento/acumulado. Um cliente com dois serviços conta duas vezes no projeto e uma vez na série da aula.

### 4. Contrato JSON e consultas SQL não são idênticos — prioridade média

| Aula | Projeto |
|---|---|
| `resumo.totalClientes` | `resumo.clientes` |
| `resumo.faturamentoTotal` | `resumo.total` |
| `porSegmento[].media` | `porSegmento[].faturamentoMedio` |
| `topClientes[].codigo/nome/faturamento` | `codigoCliente/nomeCliente/faturamentoAnual` |
| Objeto `qualidade` | Informações parciais distribuídas em `resumo` e `rejeitadas` |
| `estatistica_lote.gerado_em` | `estatistica_lote.criado_em` |
| `cliente.id/nome/consultor_id` | `id_cliente/nome_empresa/id_consultor` |
| `contrato.cliente_id/servico_id/data_contratacao` | `id_cliente/id_servico/data_inicio` |
| Status `AGUARDANDO_ETL` | Status `PENDENTE` |

Frontend e backend do projeto usam seus próprios nomes de modo coerente. As consultas SQL e trechos do dashboard da aula não podem ser copiados integralmente sem adaptação. O projeto também não produz todos os indicadores do TXT, como coeficiente de variação e média de serviços por cliente.

### 5. Dashboard difere da demonstração — prioridade média

`DashboardView.vue` mostra clientes por consultor, enquanto o TXT mostra faturamento por consultor. A tela não repete a consulta automaticamente a cada cinco segundos enquanto um lote está pendente, como no exemplo. O backend atual aguarda a chamada Python na requisição, reduzindo a necessidade dessa atualização no fluxo normal, mas o comportamento não é igual ao material. A rota é `/app/dashboard`, em vez de `/dashboard`.

### 6. POST `/etl` sem loteId tem estados incompatíveis — problema adicional

O projeto aceita omitir `loteId` e chama `criar_lote`. Porém, `banco.py:53` cria o lote como `PROCESSANDO`, enquanto `reservar_lote`, chamado logo depois, só aceita `PENDENTE`. Pela leitura do código, o pedido retorna conflito e deixa o lote em processamento. O fluxo Spring da aula envia loteId e não depende dessa extensão. O problema foi identificado estaticamente; não foi reproduzido em banco nesta revisão.

### 7. Azure não confirmado

O código permite PostgreSQL local e Azure; os padrões apontam para `localhost/cti_insights` e SSL `prefer`. O material usa Azure e SSL `require`. Uma execução local atende ao fluxo técnico, mas não comprova a etapa de gravação em Azure. Backend e Python devem apontar para o mesmo banco.

## Validação executada

- `node --test tests/frontend/etl-aula12.test.js tests/frontend/cienciaDados.test.js`: **5 testes passaram**.
- A planilha fornecida foi lida pelo teste do frontend: **38 registros, 32 válidos e 6 inválidos**; o pedido HTTP foi validado com adaptador simulado e Bearer JWT.
- `npm.cmd run build`: **compilação concluída**, com aviso de tamanho de bundle.
- Os testes Python não concluíram em tempo útil no ambiente alternativo. Os testes existentes contêm as expectativas de 61 contratos e média de R$ 2.261.250,00, mas a existência dessas expectativas não comprova sua execução.
- Não foi realizado upload real nem consulta ao PostgreSQL/Azure. Os testes Java de integração usam servidor HTTP simulado para o Python e não foram executados nesta revisão.

## Ordem sugerida

1. Recuperar o ambiente Python e confirmar `/saude` e `/testar`.
2. Alinhar o serviço FastAPI nos arquivos de implantação.
3. Decidir se a entrega deve reproduzir exatamente o JSON, os gráficos e a série mensal da aula; adaptar os pontos acima.
4. Enviar a planilha em um banco de teste e confirmar status PROCESSADO, 32 clientes, 61 contratos, uma estatística e média 2261250.

Nenhum arquivo de implementação foi alterado nesta revisão.

## Complemento: comparação com analytics-python.zip

Referência adicional: `C:/Users/Rogerio Bertolino/Downloads/analytics-python.zip`, verificado em 12/11/2026. Leitura direta do arquivo compactado, sem substituir arquivos ou executar o código recebido.

### Resultado

O ZIP contém 12 arquivos, todos com conteúdo diferente dos correspondentes no projeto (mesmo desconsiderando finais de linha CRLF/LF). Os oito arquivos Python do ZIP passaram por análise de sintaxe com `ast.parse`; isso não comprova funcionamento da API ou do SQL. A pasta atual também possui módulos de funcionalidades anteriores que não existem no ZIP, portanto a substituição integral retiraria funcionalidades.

O ZIP confirma o formato das estatísticas descrito no TXT: `totalClientes`, `faturamentoTotal`, `qualidade`, série mensal por clientes com meses contínuos, faturamento e acumulado. Entretanto, **o ZIP altera a responsabilidade de criação do lote em relação ao TXT/PDF**: seu Python cria um lote novo, enquanto o fluxo descrito no material recebe um lote já salvo pelo Spring. As referências não são totalmente equivalentes entre si.

| Ponto | ZIP | Projeto atual | Consequência |
|---|---|---|---|
| Criação do lote | Python cria em `main.processar_lote` | Spring cria; Python recebe `loteId` | Troca direta pode criar outro lote e deixar o original pendente |
| Resposta `/etl` | Somente `ok` | `ok` e `loteId` | ZIP não informa o ID novo ao backend |
| `/testar` | Estatísticas diretamente na resposta | Envelope com `resultado`, linhas tratadas, rejeições e avisos | Consumidores/testes precisam de adaptação |
| Tabelas | `cliente.id/nome/consultor_id/segmento_id`, `contrato.cliente_id/servico_id/data_contratacao` | `id_cliente/nome_empresa/id_consultor`, segmento textual; `id_cliente/id_servico/data_inicio` | Escrita do ZIP incompatível com o schema do projeto |
| Datas do lote/estatística | `enviado_em`, `gerado_em` | `criado_em`, `processado_em` | Comandos SQL não são intercambiáveis |
| Estatísticas | Contrato do TXT, incluindo Spearman e indicadores adicionais | Contrato próprio, Pearson e testes Shapiro/qui-quadrado | Dashboard deve acompanhar qualquer alteração |
| Limpeza | Recusa segmentos fora de uma lista; normaliza nomes de serviços por dicionário | Aceita outros segmentos não vazios; reconhece mais aliases e separador de serviços `|` | A mesma entrada pode ser aceita ou recusada de forma diferente |
| Configuração | Usa `DB_*`, banco padrão `ctiinsights` | Aceita também `DATABASE_URL`, banco padrão `cti_insights` | Variáveis e nome do banco devem ser conciliados |
| Dependências | Versões mínimas (`>=`) | Versões fixadas e inclusão de requisitos do pipeline anterior | Copiar o requirements perde parte das dependências atuais |
| Amostra JSON | Dois clientes; segundo com faturamento 420000 | Quatro clientes; teste atual espera segundo com 780000 | A amostra não é a mesma; testes não podem ser transferidos sem ajustes |

### Problema encontrado no próprio ZIP

Em `analytics-python/banco.py`, linha 72 do arquivo compactado, o texto passado a `sqlalchemy.text` começa com `# Insere o cliente...` **dentro das aspas triplas SQL**. Esse comentário é enviado ao PostgreSQL como SQL; deve ser removido ou convertido em comentário SQL `--`. A sintaxe Python continua válida, portanto `ast.parse` não detecta esse erro. A falha foi identificada por inspeção, sem execução em banco.

### Recomendação

Preservar a integração e o schema atuais. Se a entrega exigir fidelidade ao material, incorporar seletivamente a série mensal e os indicadores/nomes do JSON do ZIP, adaptando a tela e mantendo a criação do lote no Spring. A gravação do ZIP precisa de adaptação ao schema e correção do comentário SQL antes de qualquer uso. O ZIP não resolve, por si só, o ambiente virtual quebrado nem a ausência da API FastAPI nos arquivos de implantação.

Este complemento é uma comparação estática. Não foi executado código do ZIP, feita migração nem alterada a aplicação.
