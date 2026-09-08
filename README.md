# CTI Insights

Pipeline de análise de dados comerciais:

`Excel → Vue 3 → Spring Boot → Python → PostgreSQL → Chart.js`

## Estrutura

- `src/`: front-end Vue 3, upload, relatórios e dashboards Chart.js.
- `backend/`: API Spring Boot responsável por validação, coordenação e persistência.
- `python/`: tratamento, padronização e cálculo das métricas.
- `compose.yml`: PostgreSQL e API para execução integrada.

## Execução do front-end

```bash
npm install
npm run dev
```

Copie `.env.example` para `.env` caso a API não esteja em `http://localhost:8080`.

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
