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
