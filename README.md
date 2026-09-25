# CTI Insights

Pipeline de análise de dados comerciais:

`Excel → Vue 3 → Spring Boot → Python → PostgreSQL → Chart.js`

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
- `python/`: tratamento, padronização e cálculo das métricas.
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
