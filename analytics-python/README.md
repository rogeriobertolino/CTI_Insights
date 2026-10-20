# Ciência de dados do CTI Insights

Implementação do guia de ETL, integrada ao backend e ao dashboard. A pasta `python/` mantém a entrada JSON chamada pelo Spring Boot. Os módulos desta pasta fazem limpeza, validação, estatística, gráficos e insights. A leitura usa openpyxl para preservar a estabilidade do fluxo; não depende de pandas. SciPy executa os testes estatísticos e Matplotlib exporta PNG.

## Executar no Windows

Recomendado Python 3.12, com ambiente científico isolado do OneDrive:

```powershell
npm run setup:analytics
npm run start
```

O script instala dependências e configura `PythonExecutable` no arquivo de configuração local. A configuração não é versionada. Reinicie a API se já estiver em execução para ela usar o novo Python.

Para executar a análise independente, sem enviar nada à API:

```powershell
& "$env:LOCALAPPDATA/CTI_Insights/analytics312-venv/Scripts/python.exe" analytics-python/analise_cti.py "caminho/planilha.xlsx"
```

As saídas ficam em `analytics-python/saidas/` e os quatro PNGs em `analytics-python/graficos/`. Ambas as pastas são ignoradas pelo Git. Use `--saida caminho` para outro destino. A planilha de 300 registros mencionada no guia não foi anexada; o programa aceita o caminho de uma planilha real e não inventa uma base comercial.

Em outro sistema, crie um ambiente virtual Python 3.12 e instale `analytics-python/requirements.txt`. O Docker do backend também inclui os módulos e dependências necessárias à importação.

## Planilha oficial

Aba preferencial `dados_importacao` (primeira aba como compatibilidade). Colunas: `codigo_cliente`, `cliente`, `consultor`, `segmento`, `nivel`, `faturamento_mensal`, `servico`, `valor_servico`, `status_contrato`, `data_inicio`. Cada linha representa um cliente e seu contrato; códigos repetidos invalidam todas as ocorrências no lote, conforme a regra do guia. Isso limita a dispersão a um serviço por cliente quando a base segue estritamente esse formato; o sistema não inventa variação nem afirma correlação.

São normalizados espaços, segmentos, níveis, status, moedas e datas. Níveis: A/B/C; status: ATIVO/PENDENTE/CANCELADO; dinheiro não negativo, até duas casas decimais; datas ISO ou DD/MM/AAAA. O zero é válido. Registros inválidos preservam os valores originais e as causas. Formatos anteriores de clientes continuam compatíveis; não recebem artificialmente informações de contratos ausentes.

## Integração

- `POST /api/v1/imports`: XLSX autenticado. Spring executa Python, recebe JSON, valida novamente e salva o lote, registros válidos, clientes novos, serviços, contratos e insights. Lotes existentes permanecem intactos. Clientes existentes não são sobrescritos; contratos equivalentes não são duplicados.
- `GET /api/v1/imports?page=0&size=25`: histórico; `GET /api/v1/imports/{uuid}`: resultado do lote; `GET /api/v1/dashboard/latest`: compatível com o dashboard existente.
- `POST /api/analises/resultado`: caminho opcional Python → HTTP, restrito a ADMIN. Recebe `{importacaoId: UUID, estatisticas: {...}, insights: [...]}` para um lote concluído existente. O UUID real substitui o número fixo 1 do exemplo didático. Mantém a análise original e armazena o complemento em `externalResult`; reenvio substitui os insights desse complemento.

Para usar o cliente HTTP independente, defina `CTI_API_URL` e `CTI_API_TOKEN` no ambiente e acrescente `--enviar UUID_DO_LOTE`. O envio é explícito, com timeout e sem redirecionar o token. Use HTTPS fora da máquina local. Não há gravação direta do Python no PostgreSQL nem callback HTTP recursivo durante o upload gerenciado pelo Spring.

## Resultados e interpretação

Exporta CSVs de válidos, inválidos, estatísticas por segmento/nível e indicadores, além de `insights.txt`, JSON e quatro PNGs. O dashboard fornece as mesmas séries e exportações CSV.

A variância e o desvio padrão são amostrais; para uma única observação, a análise científica informa ausência da medida em vez de inferência. Shapiro-Wilk só é aplicado entre 3 e 5.000 valores não constantes. Qui-quadrado exige ao menos dois segmentos e dois níveis; frequências esperadas menores que 5 geram aviso e impedem conclusão automática. p > 0,05 não comprova normalidade ou independência. Fontes: [Shapiro-Wilk](https://docs.scipy.org/doc/scipy/reference/generated/scipy.stats.shapiro.html), [qui-quadrado](https://docs.scipy.org/doc/scipy/reference/generated/scipy.stats.contingency.chi2_contingency.html).

A persistência usa a conexão PostgreSQL já configurada. As variáveis de ambiente existentes permitem apontar para Azure, mas este trabalho não provisiona um servidor Azure nem altera credenciais ou dados em nuvem.
