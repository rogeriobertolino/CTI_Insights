# Comparação do CTI_Insights com cti-projeto.rar

Este documento registra o diagnóstico **anterior à complementação**. A implementação e os critérios atuais estão em [Contrato da carteira anual](Contrato_Carteira_Anual.md).

Data: 29/10/2026. Referência: `C:/Users/Rogerio Bertolino/Downloads/cti-projeto.rar`. Projeto avaliado: esta pasta `CTI_Insights`, branch `appmod/java-upgrade-20260907222301`, base `8f30dfa`.

## Parecer

**O seu projeto possui a base principal, mas não contempla integralmente as informações e o comportamento do RAR.** Há equivalência em Vue/Pinia/Axios, login JWT/BCrypt, aprovação de cadastros, importação, PostgreSQL, estatísticas e gráficos. Entretanto, os contratos de dados e as regras de validação diferem. Copiar arquivos por nome não resolveria essas diferenças.

O principal resultado prático é que a planilha do RAR resulta em **38 linhas, 32 válidas e 6 inválidas nos dois validadores**, porém **quatro linhas recebem classificações diferentes**. Portanto, a igualdade das contagens não comprova compatibilidade.

Nenhum dado foi importado no banco nesta revisão. Não foram substituídos arquivos de implementação nem aplicados os scripts SQL do RAR. A extração e os experimentos ficam em `.runtime/comparacao-rar`, pasta ignorada pelo Git.

## Como foi feita a verificação

- Listagem do RAR e extração separada, excluindo dependências, compilados, cache e metadados Git.
- Confronto do README, contrato de integração, guias, esquema SQL, código de frontend/backend/Python e das seis abas da planilha `sample-data/dados.xlsx`.
- Leitura das implementações atuais correspondentes, incluindo validação, criação de clientes/contratos, análise científica e aprovação administrativa.
- Execução da store atual com a planilha original e geração do arquivo que ela enviaria ao servidor, sem fazer a requisição de importação.
- Execução isolada da função pura de validação do frontend de referência, com datas do Excel convertidas para ISO. Não foi iniciado o projeto do RAR nem executado seu teste com gravação.

Os resultados e diferenças por linha estão em `.runtime/comparacao-rar/contraste-validacao.json`; o resultado detalhado da store atual está em `.runtime/comparacao-rar/resultado-amostra-local.json`.

Limite da verificação: a execução auxiliar da análise Python atual não concluiu e foi encerrada. As conclusões sobre suas métricas e compatibilidade de campos são de inspeção de código; não constituem validação numérica completa desse pipeline. O teste de leitura e classificação dos registros no frontend foi concluído nas duas versões.

## Resultado da planilha de referência

| Linha do Excel | RAR | Seu projeto | Consequência |
| --- | --- | --- | --- |
| 32 | Inválida: faturamento vazio | Inválida | Concordância no bloqueio |
| 33 | Inválida: serviços contratados vazios | Válida | Campo obrigatório da referência não é exigido |
| 34 | Inválida: consultor vazio | Válida | Pode ser substituído por consultor genérico na persistência atual |
| 35 | Válida: nível `b` pode ser padronizado | Inválida: ocorrência de padronização | Aviso de ajuste impede envio no fluxo atual |
| 36 | Válida: primeira ocorrência de `CTI035` | Inválida: código repetido | Seu projeto bloqueia todas as ocorrências; a referência mantém a primeira |
| 37 | Inválida: repetição de `CTI035` | Inválida | Concordância no bloqueio desta ocorrência |
| 38 e 39 | Inválidas: nível `E` | Inválidas | Concordância no bloqueio |

A média das 32 linhas aceitas pelo validador de referência foi **R$ 2.261.250,00**, reproduzindo o número documentado no RAR. Os demais resultados documentados — 32 clientes, 6 segmentos, 4 consultores, 6 serviços e 61 contratos — são critérios de aceitação da referência; não foram confirmados por gravação em banco nesta revisão.

Bloquear todas as duplicatas pode ser uma política deliberada do seu projeto. A diferença precisa ser decidida e documentada, pois altera os registros selecionados mesmo quando o total coincide.

## Matriz de cobertura

| Informação ou comportamento do RAR | Situação no seu projeto | Evidência / observação |
| --- | --- | --- |
| Vue 3, Pinia e Axios | Presente | `package.json`, `src/stores`, `src/services` |
| Login JWT, BCrypt e perfis USER/ADMIN | Presente | `AuthService`, `JwtService`, `SecurityConfig` |
| Cadastro pendente com aprovação | Presente com contrato diferente | `/api/admin/cadastros`; RAR usa `/api/admin/usuarios` |
| Matrícula e turma do usuário cadastrado | Ausente no cadastro de usuário | `CadastroRequest` e `Usuario` atuais não possuem esses campos. Matrícula de Consultor é outro cadastro |
| Bloquear acesso de usuário já aprovado e reaprovar bloqueado | Ausente nesse fluxo | Atual revisa apenas PENDENTE, para APROVADO ou RECUSADO; RAR permite mudar para BLOQUEADO e reaprovar |
| Ler XLSX até 5 MB | Presente | Atual também aceita XLS e CSV, com limites de linhas/colunas |
| Priorizar a aba `upload_clientes` | Parcial | Atual lê a primeira aba; a amostra funciona porque essa aba é a primeira |
| Oito campos obrigatórios da referência | Parcial | No formato do RAR, atual exige principalmente código, nome e segmento; consultor, nível e demais regras não são integralmente equivalentes |
| Distinguir erro bloqueante e aviso de limpeza | Parcial | Atual registra ocorrências de padronização como erros para selecionar registros válidos; referência mantém linhas com aviso |
| Seis segmentos canônicos | Parcial | Atual não impõe o mesmo conjunto; Educação/Tecnologia podem permanecer em caixa alta no frontend; Python e backend usam normalizações diferentes |
| Serviços múltiplos separados por `;` | Parcial | Análise descritiva conta serviços, mas importação de contratos espera `servico` singular e outros campos |
| `data_contratacao` do RAR | Parcial | Pode ser preservada nos registros, mas criação de contrato e série temporal atuais usam `data_inicio` |
| Cidade e UF | Parcial | Podem acompanhar o registro importado, mas não são campos da entidade Cliente atual; faltam a validação/tratamento equivalentes e uso estruturado |
| Faturamento anual | Parcial | Há reconhecimento do período no frontend e estatística descritiva; módulo científico é orientado a `faturamento_mensal` |
| Média, mediana, desvio, quartis, moda | Presente, com diferença de formato de entrada | `python/analyze.py` e `analytics-python/estatistica.py`; não confundir disponibilidade das funções com cobertura da amostra anual em todos os painéis |
| Estatísticas completas por segmento, nível e consultor | Parcial | Atual módulo científico agrupa por segmento e nível; por consultor há contagem descritiva, sem o mesmo conjunto de métricas da referência |
| Ranking dos cinco maiores clientes | Não localizado como equivalente | RAR gera `topClientes`; não foi localizado produtor/consumidor equivalente no atual |
| Pearson e Spearman entre serviços e faturamento | Ausente | Atual possui dispersão, mas não os coeficientes e p-valores da referência |
| Coeficiente de variação e média de serviços por cliente | Ausente como saída equivalente | Campos presentes no resumo do RAR, não no contrato atual examinado |
| Evolução mensal contínua com acumulado | Parcial | Atual agrupa `data_inicio`, sem o mesmo preenchimento de meses sem eventos e acumulado do RAR |
| Shapiro-Wilk e qui-quadrado | Presente no seu módulo científico | Exigidos/sugeridos na aba `ciencia_de_dados`; atuais implementações verificam condições de aplicabilidade |
| Histogramas, boxplot e dispersão | Presente para o formato científico atual | Precisam também receber corretamente os campos anuais e serviços múltiplos da referência |
| Persistência e histórico de lotes | Presente com modelo diferente | Atual: `import_batch`/`ImportedRecord`, JSON e entidades; RAR: `upload_lote`/`estatistica_lote` |
| Qualidade com total original, descartadas e avisos persistidos | Parcial | Front atual envia uma nova planilha só com válidos; não transmite junto a contagem de descartados da origem como o JSON do RAR |
| Snapshot de clientes por lote | Diferente | RAR relaciona cliente ao lote e permite mesmo código em lotes distintos; atual mantém código global único e não sobrescreve cliente existente |
| Guias comentados e planilha de referência | Não integralmente presentes | A documentação atual tem conteúdo próprio; não contém todos os guias, exemplos e abas do material recebido |
| Azure/PostgreSQL | Preparação presente; deploy não comprovado | Há Bicep/guia Azure; esta revisão não conectou a nuvem nem verificou recursos provisionados |

## Diferenças de arquitetura que exigem decisão

### Quem grava no banco

No RAR, Spring valida e envia JSON ao FastAPI (`POST /etl`). Python/SQLAlchemy cria o lote e persiste clientes, serviços, contratos e estatísticas.

No seu projeto, Spring recebe XLSX (`POST /api/v1/imports`), executa Python, recebe JSON e grava usando JPA. O README do seu módulo Python confirma que ele não acessa diretamente o PostgreSQL nesse fluxo.

As duas arquiteturas podem atender a um sistema de análise. Porém, se o requisito de entrega for especificamente **Python responsável pela gravação**, seu projeto ainda não o atende. Se o requisito for o resultado funcional, é possível adaptar o formato de entrada mantendo a persistência atual em Java.

### Modelo de contrato

O RAR transforma `Internet Dedicada;Firewall` em dois contratos, vinculados à mesma `data_contratacao`. Seu `ImportDomainService.persistContract` só prossegue com `servico` e `data_inicio`, e também usa `valor_servico` e `status_contrato`.

Logo, **o arquivo do RAR não gera os 61 contratos esperados pelo importador atual sem adaptação**. Isso é diferente de dizer que seu projeto não possui contratos: ele já possui CRUD e importação para outro formato.

### Preservar um campo não significa utilizá-lo

`ImportedRecord` e o JSON de análise podem guardar informações como cidade, UF, serviços e data original. Isso evita afirmar que todo dado desaparece. Entretanto, a entidade Cliente, o relacionamento de Contrato e os indicadores não consomem esses campos da mesma forma que a referência. A lacuna está na validação, normalização, modelagem e utilização funcional.

### Contrato HTTP e esquema SQL

O RAR usa `/api/uploads/validadas` com JSON camelCase, `/api/admin/usuarios/{id}/status` e mensagens em `erro`. Seu projeto usa importação multipart, `/api/admin/cadastros` e erros com outro contrato. Os componentes das duas versões não podem ser misturados diretamente.

Não aplicar o `database/schema.sql` do RAR ao banco atual: há tabelas com nomes iguais e colunas/relacionamentos diferentes, além de `usuario` na referência versus `usuarios` no atual. A referência contém uma instrução de DROP em seu README de banco; ela não foi executada e não é uma migração apropriada para incorporar estas diferenças.

## Informações dos arquivos de documentação

| Material recebido | Cobertura no atual / diferença |
| --- | --- |
| README raiz | Fluxo geral equivalente; divisão de responsabilidades e comandos diferentes |
| `docs/CONTRATO.md` | Falta documento equivalente com o contrato específico do RAR: oito campos, JSON e estatísticas camelCase |
| `docs/FRONT-AXIOS.txt` | Parte coberta pelo upload atual, mas avisos, obrigatoriedade, rotas e envio JSON diferem |
| `docs/BACKEND.txt` | JWT e aprovação existem; faltam matrícula/turma e bloqueio/reaprovação equivalentes |
| `docs/CIENCIA-DE-DADOS.txt` e `docs/GUIA-PYTHON.txt` | Atual possui ETL e guia próprios; faltam carga direta Python/SQLAlchemy e parte das métricas/saídas específicas |
| `docs/TESTES.txt` e `testes/testar_tudo.py` | Atual possui suítes próprias; falta um cenário de aceitação para a mesma amostra e seleção de registros |
| `database/README.md` e `schema.sql` | Há configuração/migrações próprias, porém o modelo de dados é diferente |
| READMEs de frontend/backend/Python e `.env.example` | Comandos e nomes de variáveis não são intercambiáveis; não copiar `.env` real |
| `sample-data/dados.xlsx`, aba `upload_clientes` | Lida com sucesso; quatro decisões de validação divergem |
| Aba `LEIA-ME` | Orientações didáticas em parte refletidas no README atual; ela descreve uma etapa anterior, só frontend |
| Aba `dicionario_dados` | Confirma serviços, data, cidade e UF, cujos tratamentos/destinos estruturados não estão integralmente cobertos |
| Aba `futuro_azure_postgresql` | Parte arquitetural existente; nuvem permanece preparação, não implantação confirmada |
| Aba `exemplo_padronizado` | Não localizada como mesmo artefato didático no projeto atual |
| Aba `ciencia_de_dados` | Várias técnicas existem no atual, mas é preciso compatibilizar campos anuais, datas e serviços para executar esse roteiro integralmente |
| `analytics-python/amostras/clientes.json` | Não localizado como mesmo exemplo no atual; seus testes usam outras massas |

Os guias TXT reproduzem grandes trechos do código da própria referência. A avaliação considera o significado dessas regras e não apenas a existência de arquivos com o mesmo nome. Dependências, arquivos compilados e caches não foram classificados como informações funcionais faltantes.

## Pontos em que o seu projeto já vai além

- CRUDs comerciais de clientes, consultores, serviços, contratos, insights e telemetria.
- Histórico paginado de importações e consulta de registros.
- Validação de CSV/XLS além de XLSX, limites de linhas/colunas e preservação dos valores originais.
- Suítes automatizadas em Java, JavaScript e Python.
- Módulo científico com normalidade, associação, exportações CSV/PNG e condições de aplicabilidade documentadas.
- Aprovação com controle de concorrência e rastreamento da decisão administrativa.
- Infraestrutura Docker e preparação de deploy.

## Limitações da própria referência

Não é adequado tratá-la como versão final sem lacunas. Seu README diz que o dashboard é a próxima etapa, mas há uma tela `DashboardView.vue` que chama `GET /api/dashboard`; não foi encontrado controller desse endpoint no backend extraído. O dashboard do seu projeto já possui integração própria.

Também coexistem duas instruções de arquitetura no material: a planilha didática coloca a gravação sob responsabilidade futura de Spring; os guias mais recentes determinam gravação pelo Python. Convém definir qual versão do requisito vale para a entrega.

## Ordem recomendada para completar a cobertura

1. Formalizar o contrato da planilha: oito campos obrigatórios, campos opcionais, seis segmentos e diferença entre aviso e erro.
2. Definir a política de duplicatas e reproduzir o resultado esperado com identificação das mesmas 32 linhas, não somente a contagem.
3. Adaptar serviços múltiplos e `data_contratacao` ao modelo de contratos, sem inventar preço/status silenciosamente.
4. Tratar faturamento anual no módulo científico sem perda de período ou mistura com receita mensal; completar agrupamentos, ranking, correlações e série temporal.
5. Persistir cidade/UF onde forem necessárias e manter contagem original, descartadas e avisos por lote.
6. Completar matrícula/turma e bloqueio/reaprovação, caso esses requisitos façam parte da entrega.
7. Incorporar a planilha, dicionário e guias úteis, reescritos para a arquitetura escolhida, e criar um teste de aceitação sem depender de banco de produção.

O parecer é de **cobertura parcial com base funcional ampla**. O projeto atual deve ser complementado por adaptações localizadas; substituir tudo pelo RAR perderia funcionalidades já existentes e introduziria incompatibilidades de API e banco.
