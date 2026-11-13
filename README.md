# CTI Insights

Pipeline de análise de dados comerciais:

`Excel → Vue 3 → Spring Boot → Python → PostgreSQL → Chart.js`

## Aula 12 — ETL em Python com FastAPI

```text
Vue 3 ──JSON──▶ Spring Boot ──POST /etl (X-ETL-Key)──▶ FastAPI + pandas ──SQLAlchemy──▶ PostgreSQL
  ▲                 │ valida de novo, grava upload_lote (PENDENTE)              │ limpa, calcula, grava
  └── GET /api/dashboard?loteId=N ◀── Spring lê estatistica_lote ◀──────────────┘
```

1. O Vue lê a planilha, valida e envia **só as linhas válidas** em JSON para `POST /api/uploads/validadas`.
2. O Spring revalida cada linha (`ClienteDTO` + `ClienteValidacaoService`), grava o lote `PENDENTE` e publica `LoteValidadoEvent`.
3. Depois do commit, `PythonEtlService` pergunta `GET /saude` e envia `POST /etl` com a chave `X-ETL-Key`. Python desligado → lote `ERRO` com a mensagem "Não foi possível falar com o serviço Python".
4. O Python (`api.py` → `main.py`) reserva o lote (`PROCESSANDO`), limpa com pandas (`limpeza.py`), calcula (`analise.py`), escreve as frases (`insights.py`) e grava tudo numa transação (`banco.py`) → `PROCESSADO`.
5. O dashboard em **`/app/dashboard`** lê o JSON salvo via `GET /api/dashboard?loteId=N`.

| Pasta | Responsabilidade |
|---|---|
| `src/` (front) | Telas Vue: login, upload, relatório, dashboard. Valida a planilha antes de enviar. |
| `backend/` | Spring Boot: login JWT, revalida as linhas, cria o lote e chama o Python. Grava `usuarios` e `upload_lote`. |
| `analytics-python/` | FastAPI: recebe o JSON, limpa, calcula, gera as frases e grava `cliente`, `consultor`, `segmento`, `servico`, `contrato`, `estatistica_lote`. |
| `database/schema.sql` | As tabelas do PostgreSQL (local, Neon ou Azure). |

### Rodar em três terminais (como na aula)

```powershell
# Terminal 1 — Python (porta 8000)
cd analytics-python
python -m venv .venv            # uma vez (no Windows prefira criar o venv fora do OneDrive)
.venv\Scripts\Activate.ps1
pip install -r requirements.txt # uma vez
copy .env.example .env          # uma vez: preencha DB_HOST, DB_NAME, DB_USER, DB_PASSWORD, ETL_API_KEY
python testar_conexao.py        # deve mostrar "Conectou!"
uvicorn api:app --port 8000     # http://localhost:8000/saude e http://localhost:8000/docs

# Terminal 2 — Spring Boot (porta 8080). Mesmo ETL_API_KEY do Python.
cd backend
$env:ETL_API_KEY = 'mesma-chave-do-python'; $env:JWT_SECRET_BASE64 = '...'; $env:DATABASE_PASSWORD = '...'
.\mvnw.cmd spring-boot:run

# Terminal 3 — Vue
npm install
npm run dev
```

Neste computador, `npm start` já sobe os três (Python em `127.0.0.1:8000`) com a mesma chave `ETL_API_KEY`, guardada com DPAPI em `%LOCALAPPDATA%\CTI_Insights`. O Python leva de 1 a 2 minutos para carregar pandas/SciPy; confira com `npm run status`. Se você já roda `npm run dev` à parte, use `npm run start:api` (banco, Python e backend, sem o front).

Teste com `sample-data/dados.xlsx`: **38 linhas, 32 válidas, 6 com erro, 61 contratos, média R$ 2.261.250,00**. Conferência no DBeaver (troque `1` pelo id do lote):

```sql
SELECT id, status, mensagem FROM upload_lote ORDER BY id DESC;          -- PROCESSADO
SELECT COUNT(*) FROM cliente  WHERE lote_id = 1;                        -- 32
SELECT COUNT(*) FROM contrato WHERE lote_id = 1;                        -- 61
SELECT resultado::json -> 'resumo' ->> 'media' FROM estatistica_lote WHERE lote_id = 1;  -- 2261250.0
```

As tabelas `cliente`, `consultor`, `servico` e `contrato` são as mesmas das telas de gestão comercial; reenviar a mesma planilha atualiza os clientes (por `codigo_cti`) sem duplicar contratos. Outros formatos de planilha (carteira anual, contratos com valor) continuam no fluxo anterior (`POST /api/v1/imports`).

### Aula 10 — roteiro de testes JWT

Por padrão, um cadastro novo fica **pendente** até um admin aprovar (`/app/admin/cadastros`) e o login devolve 403 até lá. Para seguir os 4 testes do Thunder Client exatamente como no slide (cadastro 201 → login 200), suba o backend com `CADASTRO_REQUER_APROVACAO=false`. O backend compila com `--release 21` (roda em JDK 21 ou 25) e tem Maven Wrapper (`.\mvnw.cmd clean install`).

## Atividade individual — validação de planilha com Pinia

A atividade utiliza Vue 3 e Pinia no fluxo **Excel → `/app/upload` → store → `/app/relatorios`**. A leitura e a validação acontecem no navegador; essa funcionalidade funciona sem Java, Python ou banco de dados.

```bash
npm install
npm run dev
```

Abra o endereço informado pelo Vite e acesse `/app/upload`. Baixe o modelo Excel com dois registros válidos, preencha a planilha e envie o arquivo. A tela também oferece uma demonstração explícita com sete registros para experimentar os erros. Os dados ficam somente na memória da sessão: atualizar a página ou limpar a análise apaga o resultado temporário.

### Formato e validações

- Arquivo `.xlsx` de até 5 MiB, com análise da primeira aba, no máximo 10.000 registros e 100 colunas. A área utilizada deve terminar até a linha 10.001 e a coluna CV.
- Cabeçalhos obrigatórios: `codigo`, `nome` e `email` no modelo de clientes. Bases comerciais com `codigo`, `nome` e `segmento` também são aceitas, com e-mail opcional. Também são aceitos aliases como `Código`, `Código Cliente`, `Nome Cliente` e `E-mail`. Cabeçalhos ausentes, vazios ou duplicados impedem a importação.
- `status` e `nivel` são opcionais. Quando preenchidos, aceitam respectivamente `Ativo`, `Inativo` ou `Pendente`, e `A`, `B` ou `C`.
- São identificados campos obrigatórios vazios, espaços no início/fim ou repetidos, e-mails inválidos, nomes e códigos fora do padrão, além de textos que precisam de padronização.
- A duplicidade considera o código, ignorando maiúsculas/minúsculas e espaços extras, e marca todas as linhas envolvidas. Nomes totalmente em maiúsculas ou minúsculas e variações de caixa em status/nível geram sugestões de revisão.
- Linhas completamente vazias são ignoradas; linhas que contêm apenas espaços são analisadas. Os números das linhas do Excel e os valores originais são preservados. As sugestões não alteram o arquivo nem corrigem os dados automaticamente.

### State, getters e actions

A store está em [`src/stores/planilha.js`](src/stores/planilha.js), com as três partes identificadas no código.

| Parte | Propriedades ou métodos | Responsabilidade |
| --- | --- | --- |
| State | `nomeArquivo`, `nomeAba`, `cabecalhos`, `registros`, `erros`, `carregando`, `erroArquivo`, `validadoEm` | Guardar os dados lidos, as ocorrências e a situação da análise temporariamente. |
| Getters | `totalRegistros`, `totalErros`, `totalComErro`, `totalValidos`, `percentualValidos`, `errosPorTipo`, `registrosValidos` | Calcular totais, percentual de aprovação, ocorrências por tipo e lista de registros sem erros. |
| Actions | `importarArquivo`, `validarDados`, `limpar` | Ler e conferir o arquivo, executar novamente as regras sem acumular ocorrências e limpar a análise. |

O relatório apresenta nome do arquivo, aba, data da validação, total de registros, válidos, registros com erro e ocorrências por tipo. Cada ocorrência identifica linha, campo, valor original, descrição e sugestão quando houver. **Registros com erro** conta linhas distintas; **total de erros** conta ocorrências, pois uma mesma linha pode ter vários problemas.

A leitura utiliza SheetJS `0.20.3`, fixado em `package.json` pelo CDN oficial. A [documentação de instalação do SheetJS](https://docs.sheetjs.com/docs/getting-started/installation/nodejs/) informa que o pacote `xlsx` do registro público npm permanece na versão antiga `0.18.5`; mantenha a origem declarada neste projeto.

### Entrega

O relatório escrito da atividade está em [`docs/Relatorio_Atividade_Pinia.docx`](docs/Relatorio_Atividade_Pinia.docx), com campos para preencher nome e data. O repositório configurado é [CTI_Insights no GitHub](https://github.com/rogeriobertolino/CTI_Insights). Para entregar, preencha o documento e publique as alterações por commit e push; a existência do endereço remoto não significa que as alterações locais já foram publicadas.

O pipeline com servidor continua disponível em `/app/importacao-servidor` e `/app/registros-servidor`, com dashboard em `/app/graficos`. As instruções abaixo descrevem esse ambiente integrado.

## Estrutura

- `src/`: front-end Vue 3, upload, relatórios e dashboards Chart.js.
- `backend/`: API Spring Boot responsável por validação, coordenação e persistência.
- `analytics-python/`: serviço FastAPI do ETL (Aula 12) e análises independentes.
- `python/`: script chamado pelo fluxo anterior de importação (`POST /api/v1/imports`).
- `database/schema.sql`: tabelas do PostgreSQL.
- `compose.yml`: PostgreSQL e API para execução integrada.

## Uso local neste computador

O ambiente local foi preparado com Java 25, PostgreSQL 17 e Python 3.13. Na raiz do projeto:

```powershell
npm.cmd start
npm.cmd run status
npm.cmd stop
```

Acesse **http://127.0.0.1:5174/app/importacao-servidor**. A API usa `127.0.0.1:8081` e o PostgreSQL usa `127.0.0.1:5433`. O comando `start` inicia os três serviços em segundo plano e verifica a saúde; `stop` encerra somente os processos gerenciados pelo CTI e preserva os dados. Execute `start` novamente depois de reiniciar o computador.

O script `scripts/local.ps1` lê a configuração específica desta máquina em `%LOCALAPPDATA%\CTI_Insights\config.json`. O banco fica em `%LOCALAPPDATA%\CTI_Insights\postgres-data`, fora da pasta sincronizada do OneDrive; a senha é protegida pelo Windows para o usuário atual. Os logs ficam em `%LOCALAPPDATA%\CTI_Insights\runtime`. A configuração e os programas locais não são incluídos no Git. Em outra máquina, prepare as dependências conforme as opções abaixo antes de configurar o script.

Se alterar o backend Java, gere novamente o JAR em `backend/target` usando Maven com Java 25 e reinicie os serviços. A `.venv` contém as dependências de `python/requirements.txt`.
## Execução do front-end

```bash
npm install
npm run dev
```

Copie `.env.example` para `.env`. O front-end usa `VITE_API_URL=/api/v1`; o proxy do Vite encaminha essas chamadas para `API_PROXY_TARGET` (padrão: `http://127.0.0.1:8080`). Reinicie o Vite depois de alterar o destino. Essa variável configura o Vite; o Nginx do Compose usa o destino definido em `nginx.conf`.

## Execução integrada

Com Docker instalado:

```bash
docker compose up --build
```

A API estará em `http://localhost:8080`, o PostgreSQL na rede interna do Compose e o front-end em `http://localhost:5173`. O Nginx encaminha `/api` ao Spring Boot.

### Recuperação de senha

Na tela de login, use **Esqueci minha senha** para solicitar um link enviado ao e-mail da conta. O link expira em 30 minutos e só pode ser usado uma vez. Para habilitar a entrega, configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` e `RESET_PASSWORD_URL` no ambiente do backend (ou no `.env` usado pelo Compose). Sem SMTP, a API mantém a resposta genérica, mas não envia e-mail.

### Desenvolvimento no Windows

Com o Docker Desktop em execução, configure `POSTGRES_PASSWORD` no arquivo local `.env` antes da primeira inicialização. O banco usa um volume persistente; alterar essa variável depois não altera a senha de um banco já criado.

Para usar o front-end pelo Vite e o backend pelo Docker:

```powershell
docker compose up -d --build backend postgres
npm.cmd run dev
```

Não inicie o serviço `frontend` do Compose enquanto o Vite estiver usando a porta 5173. Java 25 e as dependências Python são instalados nas imagens, sem necessidade de instalação local. O PostgreSQL fica acessível à API pela rede interna do Docker.

Verifique os serviços e a saúde da API:

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
docker compose logs --tail=100 backend
```

Para parar os contêineres preservando o banco, use `docker compose stop`.

## Execução sem Docker

1. Crie o banco PostgreSQL `cti_insights`.
2. Instale Python 3, crie um ambiente virtual e execute `pip install -r python/requirements.txt`.
3. Configure `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `PYTHON_EXECUTABLE` e `PYTHON_SCRIPT`.
4. Na pasta `backend`, execute `mvn spring-boot:run`.
5. Na raiz, execute `npm run dev`.

## API

- `POST /api/v1/imports`: recebe um campo multipart `file` com XLSX de até 20 MB.
- `GET /api/v1/dashboard/latest`: devolve a análise persistida mais recente.
- `GET /actuator/health`: informa a saúde da API.

O arquivo bruto é temporário e removido ao fim do processamento. O PostgreSQL mantém o lote, as métricas e cada registro tratado. A resposta da API limita a prévia a 1.000 registros para proteger o navegador.

## Erro 501 ao importar

Um `501 Unsupported method ('POST')` com resposta HTML indica que o upload chegou a um servidor que não implementa a API de importação, como `python -m http.server`. Esse servidor apenas publica arquivos; o processamento depende do Spring Boot e do PostgreSQL.

Confira se o endereço configurado é realmente o backend:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

O resultado esperado é JSON com `status: UP`. Uma página HTML ou um erro 404 não confirma a API. Inicie o backend e o banco usando uma das opções acima.

Se outro projeto estiver usando as portas 8080 e 5173, execute o backend em uma porta livre. Por exemplo, para execução sem Docker, depois de configurar o banco e o Python:

```powershell
# No terminal do backend, ajuste para a pasta do seu JDK 25:
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.4.1'
$env:PORT = '8081'
Set-Location backend
mvn spring-boot:run
```

No `.env` da raiz, mantenha `VITE_API_URL=/api/v1` e defina `API_PROXY_TARGET=http://127.0.0.1:8081`. Em outro terminal na raiz:

```powershell
npm.cmd run dev -- --port 5174 --strictPort
```

Acesse o CTI em `http://localhost:5174` e confirme a saúde em `http://localhost:8081/actuator/health`. O proxy permite manter a API no mesmo endereço de origem visto pelo navegador. Não use um servidor estático isolado para substituir o backend. Após corrigir a conexão, o botão **Tentar novamente** permite reenviar a planilha selecionada.
