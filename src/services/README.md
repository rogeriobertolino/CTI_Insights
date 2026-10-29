# Integração da aula 10

O fluxo da aula 9 foi ampliado: Vue recebe a planilha, Pinia valida e prepara uma cópia dos registros válidos, e `src/services/analysis.js` envia o XLSX por Axios ao Spring Boot. A API revalida, executa Python/openpyxl e persiste no PostgreSQL antes de retornar os indicadores ao dashboard Chart.js.

`/app/upload` faz a validação local; `/app/importacao-servidor` revisa o resumo e confirma o envio dos registros válidos ao servidor. As etapas compartilham os dados temporários da store, sem repetir o formulário. A validação local funciona sem servidor; o envio exige backend e login. A API de autenticação JWT e cadastro é consumida por `src/services/auth.js` e `src/stores/auth.js`.

A preparação para Azure está em `deploy/azure/`. O template foi compilado, mas não houve publicação nem criação de recursos na assinatura.
