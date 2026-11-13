# PostgreSQL Azure e ETL

Servidor: `ctiservidorrogerio.postgres.database.azure.com`, banco `cti_provedor`. As credenciais ficam em `analytics-python/.env`, ignorado pelo Git.

`npm start` e `npm run start:api` passam a usar o Azure automaticamente quando o `.env` aponta para um host `*.postgres.database.azure.com`. A mesma configuração é entregue ao Spring e ao FastAPI, com SSL obrigatório. Para optar pelo banco local: `powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/local.ps1 start -BancoLocal`.

Se os serviços já estavam abertos antes da mudança, encerre os serviços gerenciados com `npm run stop` e inicie novamente com `npm start`. Um processo já iniciado mantém suas próprias variáveis de ambiente.

## Migração aplicada em 12/11/2026

`database/azure_etl_compatibilidade.sql` adiciona os campos de cliente e as tabelas `servico`, `contrato`, `upload_lote`, `segmento`, `estatistica_lote` e `usuarios`. As tabelas anteriores `servicos`, `contratos`, `insights` e `telemetrias` são preservadas. Elas estavam vazias no momento da inspeção. Os dois consultores originais foram preservados.

`cliente.email` passou a ser opcional, como no DTO do projeto. `consultor.senha` foi preservada, mas deixou de ser obrigatória: a autenticação atual usa `usuarios.senha_hash`. Nenhuma senha existente foi substituída. A geração de IDs é configurada apenas se não houver identity/default.

O executor `scripts/azure-database.py` salva um snapshot JSON dos dados das seis tabelas antigas e dos metadados de colunas/restrições em `.runtime/backups/` antes da transação. Esse arquivo contém dados privados; não deve ser compartilhado nem commitado. É um snapshot dos objetos inspecionados, não um dump integral do servidor.

Consulta de configuração e tabelas, sem mudanças:

```powershell
.\.venv-analytics\Scripts\python.exe scripts/azure-database.py
```

O executor aceita `--sql caminho.sql` para consultas somente de leitura e `--apply` para aplicação explícita em transação com snapshot prévio. A migração do Azure fica fora das migrações locais porque depende da estrutura anterior desse servidor.

## Validação

Conexão com SSL e ETL real validados no Azure: 38 linhas recebidas, 32 clientes válidos, 61 contratos, status PROCESSADO e média 2261250. O teste rodou em uma transação externa, com savepoints para as etapas do ETL, e terminou com ROLLBACK. Os registros de teste não foram mantidos; sequências podem apresentar lacunas, comportamento normal do PostgreSQL.

O backend foi recompilado e iniciado temporariamente com o Azure; `/actuator/health` respondeu `UP`. O Hibernate criou/atualizou também as tabelas auxiliares previstas pelas entidades Java. O processo de teste foi encerrado após a validação. Os 18 testes Python e o teste de saúde local do frontend passaram; os scripts foram verificados quanto à sintaxe.

Referência: [ALTER TABLE no PostgreSQL](https://www.postgresql.org/docs/current/sql-altertable.html).
