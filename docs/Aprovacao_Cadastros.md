# Aprovação administrativa de cadastros

Página: /app/admin/cadastros. Somente usuários ADMIN aprovados podem abrir a página e usar a API de revisão.

## Fluxo

1. O visitante solicita uma conta em /cadastro.
2. A API cria o usuário com perfil USER e situação PENDENTE, independentemente de campos extras enviados pelo navegador.
3. O login fica bloqueado, com mensagem de que aguarda aprovação.
4. O administrador entra e abre Administração > Autorizar cadastros.
5. Ao aprovar, a conta passa a APROVADO e pode entrar com a senha cadastrada.
6. Ao recusar, a conta passa a RECUSADO e continua sem acesso. A lista registra quando e por qual administrador a decisão foi tomada.

A página tem filtros por situação, contadores e paginação de 25 registros. A aprovação/recusa aplica-se apenas a solicitações pendentes; uma segunda decisão retorna conflito e a lista é atualizada. Contas ADMIN não podem ser alteradas por essa tela. A API não expõe senhas nem hashes.

## Conta administradora inicial

Foi indicada a conta existente rogeriolemos81@gmail.com, de Rogerio Bertolino Lemos, para receber ADMIN. A senha existente é mantida. Não há criação automática de administrador com senha padrão, nem possibilidade de escolher ADMIN no cadastro público.

Os usuários anteriores à funcionalidade recebem APROVADO pelo default da nova coluna. Os novos cadastros públicos recebem PENDENTE explicitamente.

## API

- GET /api/admin/cadastros?status=PENDENTE&pagina=0: lista paginada e totais por situação; omitir status lista todos os usuários USER.
- PATCH /api/admin/cadastros/{id}, JSON {"decisao":"APROVADO"} ou {"decisao":"RECUSADO"}: revisa uma solicitação pendente.
- Somente ADMIN: 401 sem autenticação, 403 para usuário sem permissão.
- 400 para decisão/parâmetro inválido, 404 para cadastro inexistente e 409 se já foi analisado.
- O registro é bloqueado para escrita durante a revisão, impedindo decisões concorrentes sobre a mesma solicitação.
- Login e filtro JWT consultam a situação atual no banco. Uma conta pendente ou recusada não acessa a API mesmo com um JWT anteriormente emitido.

## Verificações

Cadastros e aprovações são testados com H2 em CadastroAdminIntegrationTest, além da suíte JWT existente. Os testes do frontend cobrem listagem com Bearer, aprovação, recusa, bloqueio de cliques duplicados, conflitos e descarte de acesso bloqueado.

O teste Python completo foi aprovado após a correção do processamento XLSX com openpyxl.


## Resultado local

- rogeriolemos81@gmail.com confirmado como ADMIN/APROVADO, preservando a senha existente.
- 15 testes Java aprovados: 9 da suíte JWT e 6 da aprovação administrativa.
- 45 testes JavaScript aprovados na suíte completa, seguidos dos 5 testes administrativos após o ajuste final de descarte de respostas antigas.
- Frontend compilado e endpoints da página servidos pelo Vite.
- HTTP real: cadastro PENDENTE; login bloqueado antes da aprovação; administrador lista, aprova e recusa; aprovado entra; usuário comum recebe 403 ao tentar revisar; segunda decisão retorna 409.
- Contas de teste removidas. A verificação antes/depois confirmou a preservação de ids, emails e hashes dos cadastros anteriores.
- Backup anterior à atualização em .runtime/backups/cti-pre-approval.dump, fora do Git.
