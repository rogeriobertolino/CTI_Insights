# Carteira anual — contrato integrado

O formato do material `cti-projeto.rar` está integrado ao CTI Insights sem substituir o modelo de contratos mensais já existente. A amostra original, incluindo as abas didáticas e o dicionário de dados, está em [`sample-data/dados.xlsx`](../sample-data/dados.xlsx).

## Entrada

A aba `upload_clientes` tem prioridade sobre a primeira aba do arquivo. XLSX, XLS e CSV continuam disponíveis no navegador, com limite de 5 MiB, 10.000 registros e 100 colunas. O envio integrado produz XLSX.

| Campo | Obrigatório | Regra |
| --- | --- | --- |
| `codigo_cliente` | Sim | Até 60 caracteres; letras, números, `_` ou `-`. Comparação sem diferença de caixa; mantém a primeira ocorrência |
| `nome_cliente` | Sim | Até 180 caracteres, compatível com o cadastro existente |
| `consultor` | Sim | Até 120 caracteres, espaços removidos e capitalização padronizada |
| `segmento` | Sim | Indústria, Comércio, Serviços, Saúde, Educação ou Tecnologia; aceita variantes reconhecidas como `IND.` |
| `nivel_cliente` | Sim | A, B ou C; aceita minúsculas e converte para maiúsculas |
| `faturamento_anual` | Sim | Número não negativo, menor que um trilhão, até duas casas decimais; aceita moeda e formatos BR/US |
| `servicos_contratados` | Sim | Serviços separados por `;`, até 400 caracteres; remove vazios e repetições |
| `data_contratacao` | Sim | Data válida a partir de 1990, ISO ou DD/MM/AAAA; aceita células de data do Excel |
| `cidade` | Não | Até 120 caracteres, padronizada e persistida |
| `uf` | Não | Duas letras, convertidas para maiúsculas |

Espaços extras, caixa do nível e outras padronizações são **avisos** e não bloqueiam a linha anual. Obrigatórios vazios, datas/valores inválidos e códigos repetidos são **erros**. O navegador mantém originais, resultados tratados e números reais das linhas. Somente registros válidos são enviados pelo fluxo da tela. Os modos anteriores mantêm suas próprias regras de duplicidade/ocorrências.

## Fluxo e persistência

`Vue/Pinia → Axios → Spring → Python → Spring/JPA → PostgreSQL`

Foi preservada a arquitetura existente: Python calcula e Java persiste. O projeto não passa a depender de FastAPI/SQLAlchemy nem de acesso direto do navegador ao banco. Isso adapta a referência funcional à aplicação atual; os endpoints do outro projeto não devem ser misturados com os desta aplicação.

- `POST /api/v1/imports`: multipart com `file`. A tela também envia `linhasRecebidas`, `linhasDescartadas` e `linhasComAviso`. O servidor verifica limites e consistência das contagens; sua origem informada pelo navegador fica identificada em `origem`.
- `GET /api/v1/dashboard/latest`: última análise persistida. O histórico e a consulta dos lotes continuam disponíveis.
- O backend revalida a saída Python antes de persistir. O resultado anual é identificado por `analytics.schema = carteira-anual-v1` e `analytics.revenuePeriod = anual`.
- Cada serviço separado por `;` gera um contrato com a data informada. Preço não informado é `null`, nunca zero; a situação inicial é `PENDENTE`, para posterior complementação comercial. Não se infere contrato ativo nem receita contratual a partir do faturamento anual do cliente.
- Reenvios preservam clientes existentes e não duplicam contratos equivalentes. Cada lote mantém seus próprios registros e resultados como snapshot. O mesmo código de cliente continua único na carteira global.
- Cidade e UF estão na entidade Cliente e na API/cadastro comercial. Dados inválidos da amostra anual não são persistidos como registros; suas contagens ficam no lote. As linhas inválidas permanecem no navegador para correção.

## Análises

O resultado anual inclui média, mediana, moda, desvio e variância amostrais, extremos, amplitude e quartis; estatística por segmento, nível e consultor; frequência/percentual dos serviços; cinco maiores clientes; média de serviços por cliente; coeficiente de variação; evolução mensal com meses sem eventos e acumulado; distribuição por cidade/UF; Pearson e Spearman com p-valores, além dos testes de normalidade e associação já existentes.

O painel exibe o período anual e as séries de histograma, boxplot, dispersão e evolução temporal. Valores sem aplicabilidade permanecem ausentes; correlação não implica causalidade. O módulo de exportação gera CSV, JSON, insights e PNG com o período correto.

## Usuários

Cadastro aceita `matricula` (até 30) e `turma` (até 60), opcionais. Novas contas continuam USER/PENDENTE. A página administrativa permite aprovar/recusar pendentes, bloquear aprovados e reaprovar recusados/bloqueados. Perfis ADMIN e a própria conta não podem ser alterados por essas ações. Cada mudança registra administrador/data e usa bloqueio de concorrência; tokens de contas bloqueadas são recusados.

## Atualização de banco

Migração aditiva: `deploy/migrations/V20261029_01__cobertura_carteira_anual.sql`. Ela acrescenta cidade/UF e matrícula/turma, permite preço desconhecido e amplia a restrição de status para BLOQUEADO. Execute pelo procedimento existente (`npm.cmd run migrate`), que faz backup antes de aplicar migrações. Não aplique o schema nem o DROP TABLE do projeto de referência ao banco atual.

## Critérios de aceitação

A amostra de 38 registros deve selecionar as mesmas 32 linhas válidas e rejeitar as linhas **32, 33, 34, 37, 38 e 39**. Resultado: 4 consultores, 6 segmentos, 6 serviços, 61 contratos identificados, média anual **R$ 2.261.250,00** e mediana **R$ 1.470.000,00**.

- Frontend: `npm.cmd test` e `npm.cmd run build`.
- Python: `python -m unittest discover -s tests/analytics -p "test_*.py"` e `python -m unittest discover -s tests/python -p "test_*.py"`.
- Backend: `mvn.cmd -f backend/pom.xml test`.
- Integração real em banco H2 isolado: defina `CTI_FULL_PIPELINE=true` e `CTI_TEST_PYTHON` apontando para o Python com as dependências instaladas e execute os testes Java. A suíte anual verifica 32 clientes/61 contratos, snapshots por lote, preço desconhecido e reenvio sem duplicidade. Ela não usa o PostgreSQL local ou em nuvem.

Azure continua configurável pelo mecanismo existente. Esta integração não publica nem provisiona recursos em nuvem.
