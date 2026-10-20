# Autenticação JWT - Aula 10

O projeto implementa JJWT 0.12.6, BCrypt, DTOs, AuthService, JwtService e JwtAuthenticationFilter. O frontend usa JSON para cadastro/login e Authorization Bearer nas requisições protegidas.

> Atualização: novos cadastros agora precisam de aprovação administrativa antes do login. Veja [aprovação de cadastros](Aprovacao_Cadastros.md).

## Endpoints

| Método e rota | Acesso | Resultado |
| --- | --- | --- |
| POST /api/auth/cadastro | Público | 201, usuário criado com USER |
| POST /api/auth/register | Público | Alias compatível do cadastro |
| POST /api/auth/login | Público | 200, token, tipo Bearer, expiraEmSegundos e usuario |
| GET /api/auth/me | USER ou ADMIN | Usuário autenticado sem senha/hash |
| GET /api/privado | USER ou ADMIN | 200 com JWT válido; 401 sem autenticação |
| GET /api/admin | ADMIN | 403 para USER; 200 para ADMIN |
| POST /api/auth/logout | USER ou ADMIN | 204; o cliente descarta o token |

Cadastro recebe nome, email e senha. Login recebe email e senha. Ambos usam Content-Type application/json. O cadastro público não aceita promoção de perfil: mesmo se o cliente enviar perfil ADMIN, o usuário é criado como USER.

A API valida assinatura HS256, emissor, expiração, identificador e existência do usuário. A autoridade é consultada no banco a cada requisição, portanto alterações de perfil valem imediatamente. Token inválido ou expirado retorna 401; falta de permissão retorna 403. Cookies e sessões anteriores não autenticam a API.

## Dados existentes

A entidade Usuario adiciona perfil com DEFAULT USER e NOT NULL. Na atualização do esquema pelo Hibernate, os usuários já cadastrados recebem USER. Ids, nomes, emails e hashes BCrypt permanecem existentes. Não há administrador padrão ou senha de administrador embutida.

Para promover um usuário, um administrador do banco pode executar, após conferir o email:

```sql
UPDATE usuarios SET perfil = 'ADMIN' WHERE email = 'email-do-administrador';
```

O sistema não promove ninguém automaticamente. A configuração atual usa ddl-auto=update; em ambiente com migrações controladas, aplique a coluna equivalente antes da nova versão.

## Chave e execução local Windows

`npm.cmd start` gera uma chave aleatória de 32 bytes na primeira inicialização, protegida com DPAPI em %LOCALAPPDATA%/CTI_Insights/jwt-secret.dpapi. O arquivo fica fora do Git e a chave é reutilizada nos reinícios. Uma variável JWT_SECRET_BASE64 definida explicitamente prevalece sobre esse arquivo.

Após alterar Java, gere o JAR com Maven e Java 25 antes de reiniciar a API. A aplicação recusa chave ausente, Base64 inválido ou chave com menos de 32 bytes.

Para execução direta pelo Maven, gere uma chave no PowerShell (compatível com Windows PowerShell 5.1):

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
mvn.cmd -f backend/pom.xml spring-boot:run
```

O Maven não carrega .env automaticamente. Para Docker Compose, coloque a chave gerada em JWT_SECRET_BASE64 no .env local. Não publique a chave no repositório. O Compose exige seu preenchimento.

Variáveis: JWT_SECRET_BASE64 (obrigatória), JWT_ISSUER (padrão cti-insights) e JWT_EXPIRATION_SECONDS (padrão 1800, contado desde a emissão). Azure recebe a chave pelo parâmetro seguro jwtSecretBase64, usando secretRef no backend. Todas as réplicas precisam da mesma chave e do mesmo emissor. A configuração foi preparada; não implica publicação no Azure.

## Navegador e expiração

O JWT fica no sessionStorage da aba, ou apenas em memória se o navegador bloquear esse armazenamento. Nenhuma senha é armazenada. Uma recarga restaura o acesso via /me; um 401 autenticado remove o token e redireciona para login. O botão Sair limpa o token mesmo offline.

O logout é local: uma cópia de um JWT emitido continua válida no servidor até sua expiração. Não há lista de revogação ou refresh token. A API não usa autenticação por cookie; por isso a cadeia stateless não exige CSRF. O endpoint antigo /api/auth/csrf deixou de fazer parte do fluxo.

## Testar a sequência da apostila

Use docs/Aula10_JWT.http na extensão REST Client, ou reproduza as requisições no Thunder Client. No Thunder Client, copie o token da resposta do login e escolha Auth > Bearer. Use porta 8081 neste computador; Docker e o padrão Maven usam 8080.

1. Cadastro: 201 e perfil USER.
2. Login JSON: 200 e token JWT, tipo Bearer e usuário.
3. /api/privado: 401 sem token e 200 com token válido.
4. /api/auth/me: 200 e dados sem senha.
5. /api/admin: 403 para USER e 200 para ADMIN.
6. Token inválido ou expirado: 401.

## Verificações automatizadas

```powershell
node --test
npm.cmd run build
mvn.cmd -f backend/pom.xml test
```

Os testes de autenticação usam H2 e uma chave exclusiva de testes em backend/src/test/resources/application.properties; ela não é empacotada como configuração da aplicação.

Referências técnicas: [JJWT 0.12.6](https://github.com/jwtk/jjwt/blob/0.12.6/README.adoc) e [Spring Security - sessões e modo stateless](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).


## Resultado da aplicação local em 19/10/2026

- 41 testes JavaScript aprovados e frontend compilado.
- 9 testes AuthImportIntegrationTest aprovados, incluindo JWT inválido/expirado, assinatura, emissor, perfis e upload por Bearer.
- API JWT ativada em 127.0.0.1:8081; site gerenciado em 127.0.0.1:5174 e proxy existente localhost:5173 verificado.
- Teste HTTP real através de localhost:5173: cadastro 201; login com JWT; /me e /api/privado 200; acesso anônimo e token inválido 401; USER em /api/admin 403; logout 204.
- Conta temporária removida após o teste. A comparação dos cadastros antes/depois confirmou ids, emails e hashes preservados; nenhum perfil ficou vazio.
- Backup anterior à atualização em .runtime/backups/cti-pre-jwt.dump, fora do Git.
- O teste adicional FullPipelineIntegrationTest com Python não foi aprovado: a inicialização do pandas não concluiu no limite de 120 segundos, e a limpeza encontrou um log temporário bloqueado no Windows. Um diagnóstico separado localizou a espera na carga de uma extensão nativa do pandas. Portanto, o pipeline Python completo não está certificado nesta execução; os testes JWT e o login HTTP foram aprovados.
- Azure recebeu os parâmetros JWT no template, mas não houve publicação nem validação por Azure CLI nesta execução.

A compilação isolada evita disputa com o compilador Java do VS Code:

```powershell
mvn.cmd -f backend/pom.xml "-Dcti.build.directory=$PWD/.runtime/jwt-build" -Dtest=AuthImportIntegrationTest package
```

Um segundo projeto XML que estava anexado ao POM foi preservado em docs/exemplos/pom-aula10-referencia.xml; o POM ativo mantém somente o projeto CTI.

> Atualização posterior: o processamento passou a usar openpyxl e a biblioteca padrão. O teste completo com Python foi aprovado na correção dos módulos comerciais; a limitação anterior do pandas foi resolvida.
