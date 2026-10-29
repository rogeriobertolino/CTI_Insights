# CTI Insights

Pipeline de análise de dados comerciais:

`Excel → Vue 3 → Spring Boot → Python → PostgreSQL → Chart.js`

## Aula 9 — upload e tratamento inicial

O fluxo é **Planilha → Vue Upload → Pinia → Tratamento → Prévia** em `/app/upload`. Aceita XLSX, XLS binário e CSV UTF-8 com vírgula ou ponto e vírgula. CSV preserva zeros iniciais. Use **Baixar modelo da aula 9** para um exemplo criado conforme os campos do PDF; ele não substitui o arquivo original da professora.

- **State:** `arquivo`, `dadosOriginais`, `dadosTratados`, `registros`, `erros` e `carregando`.
- **Getters:** `totalClientes`, `clientesNivelA`, `temDados`, `resumoPorSegmento` e totais de validação.
- **Actions:** `selecionarArquivo`, `validarArquivo`, `processarPlanilha`, `tratarLinha`, `tratarDados`, `validarDados` e `limpar`.
- **Tratamento:** remove espaços extras, padroniza segmentos para Indústria, Comércio e Serviços e converte nível para maiúsculas. Níveis fora de A/B/C continuam sinalizados.
- **Prévia:** código, nome, consultor, segmento, nível, total de clientes, quantidade no nível A e resumo por segmento. Totais incluem todos os registros processados, mesmo com ocorrências na origem.

O relatório `/app/relatorios` mostra a cópia tratada e os registros originais. Tratar um campo não apaga sua ocorrência de validação. Recarregar a página ou limpar remove os dados temporários.

No roteiro da aula, **backend Spring Boot, PostgreSQL e Azure entram na próxima etapa**. O upload didático funciona no navegador. O projeto já possui uma integração separada com servidor; Azure permanece como etapa futura. O Vue não acessa o PostgreSQL diretamente.

## Atividade individual — validação de planilha com Pinia

A atividade utiliza Vue 3 e Pinia no fluxo **Excel → `/app/upload` → store → `/app/relatorios`**. A leitura e a validação acontecem no navegador; essa funcionalidade funciona sem Java, Python ou banco de dados.

```bash
npm install
npm run dev
```

Abra o endereço informado pelo Vite e acesse `/app/upload`. Baixe o modelo Excel com dois registros válidos, preencha a planilha e envie o arquivo. A tela também oferece uma demonstração explícita com sete registros para experimentar os erros. Os dados ficam somente na memória da sessão: atualizar a página ou limpar a análise apaga o resultado temporário.

### Formato e validações

- Arquivo `.xlsx`, `.xls` ou `.csv` UTF-8 de até 5 MiB, com análise da primeira aba, no máximo 10.000 registros e 100 colunas. A área utilizada deve terminar até a linha 10.001 e a coluna CV.
- Cabeçalhos obrigatórios: `codigo`, `nome` e `email` no modelo de clientes. Bases comerciais com `codigo`, `nome` e `segmento` também são aceitas, com e-mail opcional. Também são aceitos aliases como `Código`, `Código Cliente`, `Nome Cliente` e `E-mail`. Cabeçalhos ausentes, vazios ou duplicados impedem a importação.
- `status` e `nivel` (ou `nivel_cliente`) são opcionais. Quando preenchidos, aceitam respectivamente `Ativo`, `Inativo` ou `Pendente`, e `A`, `B` ou `C`.
- São identificados campos obrigatórios vazios, espaços no início/fim ou repetidos, e-mails inválidos, nomes e códigos fora do padrão, além de textos que precisam de padronização.
- A duplicidade considera o código, ignorando maiúsculas/minúsculas e espaços extras, e marca todas as linhas envolvidas. Nomes totalmente em maiúsculas ou minúsculas e variações de caixa em status/nível geram sugestões de revisão.
- Linhas completamente vazias são ignoradas; linhas que contêm apenas espaços são analisadas. Os números das linhas do Excel e os valores originais são preservados. A cópia tratada recebe a padronização inicial; os problemas encontrados na origem continuam disponíveis para revisão.

### State, getters e actions

A store de upload é exportada por `src/stores/uploadStore.js`; sua implementação está em [`src/stores/uploadStore.js`](src/stores/uploadStore.js), com as três partes identificadas no código.

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

O mapa completo e os comandos de verificação estão em [Estrutura do Projeto](docs/Estrutura_do_Projeto.md).

- `src/`: front-end Vue 3, upload, relatórios e dashboards Chart.js.
- `backend/`: API Spring Boot responsável por validação, coordenação e persistência.
- `python/`: tratamento, padronização e cálculo das métricas.
- `analytics-python/`: módulos de limpeza, estatística, gráficos e insights.
- `tests/`: testes do frontend e dos dois módulos Python, separados por tecnologia.
- `scripts/`: execução local, migrações, preparação do ambiente e geração de documentos.
- `docs/`: documentação, exemplos e relatórios técnicos.
- `deploy/`: infraestrutura Azure e migrações SQL.
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


## Aula 10 — fluxo integrado

A store `src/stores/uploadStore.js` guarda dados originais, tratados, erros, loading e filtro de segmento. Os getters calculam clientes válidos, distribuição por segmento e faturamento total/médio dos registros válidos. O faturamento é opcional para modelos sem essa coluna; se presente, cada valor deve ser numérico e maior que zero. Faturamento mensal e anual mantêm seu período na planilha enviada. O campo anual de clientes converte mensal para anual (vezes 12); o dashboard mostra o período original importado.

As rotas `/app/upload` e `/app/importacao-servidor` usam o mesmo formulário. A action `enviarDados` exporta uma cópia XLSX tratada somente com os registros válidos, preserva a linha de origem e chama o service Axios. O backend revalida as linhas, executa Python/openpyxl e salva a análise no PostgreSQL. Ao concluir, a tela abre o dashboard Chart.js. O filtro de segmento muda apenas a prévia; o envio inclui todos os segmentos aprovados.

O cadastro em /cadastro salva usuários com senha BCrypt e perfil USER. O login retorna JWT Bearer com expiração padrão de 30 minutos. O Vue usa sessionStorage por aba e a API autentica por Authorization, sem sessão por cookie. Perfis USER/ADMIN são persistidos; /api/admin exige ADMIN. Todos os usuários autenticados acessam a mesma carteira compartilhada. Consulte [o guia da Aula 10](docs/Autenticacao_JWT_Aula_10.md) para chave, execução e testes, e [as requisições de exemplo](docs/Aula10_JWT.http).

Para usar as mudanças, gere novamente o JAR com Java 25 (`mvn -f backend/pom.xml package`) e reinicie os serviços locais. Cadastre sua conta; as credenciais antigas simuladas não correspondem a usuários reais.

Validação: `npm test`, `npm run build`, testes Python em `tests/python/test_analyze.py` e `mvn -f backend/pom.xml test`. Para executar também o teste do pipeline com Python real, defina `CTI_FULL_PIPELINE=true` e `CTI_TEST_PYTHON` com o executável do ambiente que contém as dependências de `python/requirements.txt`. Os testes Java usam H2 isolado; não escrevem no banco local.

A preparação para Azure está em `deploy/azure/README.md` e `deploy/azure/main.bicep`. A publicação requer os recursos e as credenciais da assinatura.

## Aprovação de novos cadastros

Novas contas ficam pendentes até aprovação por ADMIN em /app/admin/cadastros. A conta administradora indicada mantém sua senha atual. Consulte [o guia de aprovação](docs/Aprovacao_Cadastros.md).


### Estrutura do backend Java

O backend organiza suas classes por camada no pacote `com.cti.provedor`:

```text
backend/src/main/java/com/cti/provedor/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
├── config/
├── security/
└── CtiProvedorApplication.java

backend/src/main/resources/
└── application.properties
```

Controllers recebem as requisições HTTP; services aplicam regras de negócio;
repositories acessam o banco via Spring Data JPA; model contém entidades e enums;
dto define os dados de entrada e saída; config reúne a configuração do Spring Security; security implementa o JWT.
Os testes Java ficam em `backend/src/test/java/com/cti/provedor/`.
