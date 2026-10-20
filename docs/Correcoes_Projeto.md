# Correções do CTI Insights — 19/10/2026

Correções aplicadas ao projeto desta pasta. A pasta de referência em Des_Backend foi somente consultada e permanece intacta.

## Funcionalidades e padrão

- CRUDs com DTOs, serviços transacionais e validações para consultores, serviços, contratos e insights, em `/api/v1`. Inclusão de matrícula do consultor, preço do serviço e e-mail do cliente.
- Página administrativa `/app/gestao`, com formulários, paginação, filtros de clientes e consulta de auditoria. Escritas nos novos módulos exigem ADMIN; usuários aprovados podem consultar a API comercial. A página de aprovação de usuários permanece disponível no menu administrativo.
- Contratos validam datas, valores e vínculos. Insights verificam a associação entre contrato e cliente. Exclusões com dependências retornam conflito, preservando registros relacionados.
- Respostas de erro padronizadas, recursos ausentes com 404, validações com 400 e conflitos com 409. Paginação limitada a 100 itens; interface utiliza 25.
- Importação mantém transação dos dados e registra auditoria independente, inclusive quando o processamento falha. Cada importação concluída gera um insight de resumo. Mensagens de auditoria não armazenam exceções internas ou credenciais.
- Análise de planilhas reescrita com openpyxl e biblioteca padrão, eliminando o travamento observado com pandas. Mantidos indicadores estatísticos e contrato JSON, com testes para quartis e desvio padrão.
- Fontes Java formatados. Autenticação JWT e aprovação administrativa preservadas; nenhuma senha foi copiada do projeto de referência.

## Banco e execução

A migração aditiva `deploy/migrations/V20261019_01__cadastros_comerciais.sql` foi aplicada no PostgreSQL local. `npm run migrate` usa o script PowerShell que cria backup, registra versão e checksum e executa cada migração em transação. A segunda execução confirmou que a versão já estava aplicada. Não utiliza Flyway. Hibernate ainda inicializa schemas novos com `DDL_AUTO=update`, padrão atual; o parâmetro pode ser configurado para `validate` em bancos previamente preparados.

Backups locais ficam em `.runtime/backups`; não devem ser publicados, pois contêm dados do banco. A API anterior foi preservada em `.runtime/backups/cti-before-corrections.jar`. A versão compilada e validada foi ativada em `backend/target`.

Endereço ativo: http://127.0.0.1:5174/app/gestao . API: http://127.0.0.1:8081 . No momento da validação, a porta 5173 estava indisponível.

A conta rogeriolemos81@gmail.com permanece ADMIN/APROVADO com a senha existente.

## Verificação

- 24 testes Java aprovados, incluindo integração real com o processo Python.
- 49 testes JavaScript e 8 testes Python aprovados.
- Build final do frontend e pacote Maven concluídos. O Vite sinaliza um chunk principal acima de 500 kB; divisão adicional pode ser feita em trabalho posterior de desempenho.
- Teste HTTP no site ativo confirmou cadastro pendente, bloqueio antes da aprovação, aprovação administrativa, JWT, autorização, CRUDs, relacionamentos, filtros, 404, importação XLSX, persistência de e-mail, insight automático e auditoria.
- Contas e registros de teste foram removidos. Comparação de contagens e hashes de todos os registros anteriores confirmou preservação dos dados, inclusive da conta administradora.
- Verificação de espaços do diff concluída sem erros. Não houve validação visual em navegador.
