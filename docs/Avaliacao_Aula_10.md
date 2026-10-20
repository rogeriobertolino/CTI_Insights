# Avaliação do projeto frente à Aula 10 de Desenvolvimento Backend

Data: 19/10/2026. Fonte: `C:/Users/Rogerio Bertolino/Downloads/_A10DESB -2026.pptx.pdf`, 34 páginas.

> Avaliação inicial, anterior à implementação. Os itens JWT foram aplicados em seguida; consulte [o guia atualizado](Autenticacao_JWT_Aula_10.md). Os resultados finais da implementação estão nesse guia.

## Parecer

O projeto atende parcialmente ao conteúdo da apostila. Existe autenticação com Spring Security, BCrypt, sessão HTTP e CSRF, mas não existe a autenticação JWT apresentada na aula. O login funcionar no navegador não comprova atendimento ao fluxo JWT.

Esta é uma comparação do material didático com o código disponível, não uma nota acadêmica. Os comandos da apostila foram tratados como conteúdo a avaliar. Nenhuma migração de autenticação foi executada.

## Comparação dos itens

| Item da apostila | Páginas | Situação | Evidência no projeto |
| --- | --- | --- | --- |
| Java, Maven e Spring Boot 3 | 3-4 | Atendido | `backend/pom.xml`: Java 25 e Spring Boot 3.5.16. |
| Web, JPA e PostgreSQL | 9 | Atendido | Dependências em `backend/pom.xml` e datasource em `backend/src/main/resources/application.yml`. |
| Spring Security e Validation | 10 | Atendido | Dependências presentes; validação de cadastro com `@Valid` e constraints. |
| Bibliotecas JJWT | 11, 13 | Ausente | Não existem `jjwt-api`, `jjwt-impl` e `jjwt-jackson` no POM, nem outra implementação JWT identificada. |
| Entidade Usuario com id, nome, email e hash | 13, 15-16 | Atendido | `backend/src/main/java/br/com/cti/insights/auth/Usuario.java`. |
| Perfil persistido e enum USER/ADMIN | 13, 15, 22 | Ausente | Usuario não tem perfil. SecurityConfig atribui USER a todos. O record chamado Perfil é um DTO e devolve o texto fixo Usuário. |
| Repository para buscar usuário por email | 17 | Atendido | `UsuarioRepository.java`: `findByEmail` e `existsByEmail`. |
| Cadastro com verificação de duplicidade e BCrypt | 13, 20 | Atendido funcionalmente | `AuthController.java` verifica email, codifica senha e salva; regras estão no controller. |
| DTOs de cadastro e usuário sem senha | 18-19 | Parcial | Records Cadastro e Perfil dentro de AuthController atendem parte da função; faltam LoginRequest e LoginResponse com token. |
| AuthService separando regras do controller | 20, 24-25 | Ausente | Cadastro está em AuthController; login é processado pelo formLogin do Spring Security. |
| Verificação de email e senha | 13, 20 | Atendido por mecanismo diferente | Spring Security usa UserDetailsService e BCrypt; não há o AuthService com matches apresentado na aula. |
| Login JSON retornando JWT, tipo Bearer e usuário | 19, 28 | Ausente | Login recebe parâmetros de formulário, devolve mensagem e usa sessão; frontend consulta /me em seguida. |
| JwtService: geração, assinatura e expiração | 13, 21 | Ausente | Classe ou mecanismo equivalente não encontrado. |
| JwtAuthenticationFilter e contexto de segurança | 13, 21-22, 24 | Ausente | SecurityFilterChain existe, porém sem filtro JWT e sem validação de Bearer. |
| Rotas públicas e protegidas | 22 | Parcial em relação à aula | Há proteção por sessão e papel USER em /api/**. Falta autenticação JWT e diferenciação de ADMIN. |
| Chave JWT por variável de ambiente | 23, 26 | Ausente | JWT_SECRET_BASE64 e parâmetros de assinatura/expiração não aparecem na configuração ou nos scripts. |
| /api/auth/cadastro | 22, 27 | Contrato diferente | Projeto usa /api/auth/register; POST com JSON exige CSRF. |
| /api/privado | 22, 29 | Ausente | Não foi encontrado controller para essa rota. Outras rotas da API já são protegidas. |
| /api/auth/me sem expor senha | 30-31 | Parcial em relação à aula | Endpoint e DTO sem senha existem; autenticação atual é por sessão, não Bearer JWT. |
| Configuração, README, .env.example e .gitignore | 23 | Parcial | Arquivos existem; configuração em YAML é equivalente a properties, mas faltam parâmetros e instruções JWT. |
| Lombok citado na criação do projeto | 4 | Ausente, sem impedir a função atual | Código usa construtores e getters explícitos; não é uma lacuna funcional de autenticação. |
| Azure PostgreSQL citado na estrutura | 3 | Não comprovado em execução | Há arquivos em deploy/azure; esta avaliação não verificou recursos publicados ou conexão real ao Azure. |

## Reprodução dos testes da apostila

1. Página 27: o cadastro atual funciona pelo contrato `/api/auth/register` com CSRF. Não reproduz diretamente o POST `/api/auth/cadastro` do slide.
2. Página 28: o login atual usa formulário e sessão; não retorna JWT, tipo Bearer e usuário no mesmo corpo.
3. Página 29: não há implementação de `/api/privado`, nem validação de um JWT nessa rota. Um 401 genérico em `/api/**` não comprova a existência do endpoint.
4. Páginas 30-31: `/api/auth/me` existe e não devolve senha, mas depende de sessão. Um JWT isolado não estabelece autenticação na configuração atual.

As páginas 31 e 32 contêm repetição/fragmento visual do exemplo de consulta de usuário; não acrescentam novos requisitos identificáveis. A apostila também usa nomes Aula09 em alguns exemplos, embora a capa seja Aula 10. O nome dos pacotes não é uma diferença funcional; as responsabilidades e os contratos HTTP são.

## Atenção ao arquivo Api.js

`src/services/Api.js` contém um interceptor que lê `localStorage.token` e monta Authorization Bearer, mas não encontrei importação desse módulo no fluxo atual. Ele aponta para localhost:8080. O login real está em `src/services/auth.js`, com `withCredentials`, CSRF e parâmetros de formulário. A existência isolada desse interceptor não implementa JWT no sistema.

O record Token retornado por `/api/auth/csrf` representa um token CSRF, não um JWT de autenticação.

## Verificações executadas

- Leitura textual das 34 páginas e inspeção visual dos slides de arquitetura, estrutura, DTOs, fluxo e testes.
- Inspeção do POM, configuração, classes de autenticação, integração Vue e testes existentes.
- `node --test`: 37 testes aprovados, nenhuma falha.
- `mvn.cmd -f backend/pom.xml -Dtest=AuthImportIntegrationTest test -q`, com o Java local configurado: 4 testes aprovados, nenhuma falha ou erro. Usou H2 em memória, sem alterar o banco PostgreSQL da aplicação.
- Os testes do backend cobrem cadastro, senha não exposta, duplicidade, entrada inválida, login incorreto/correto, sessão, logout, proteção e importação. Não comprovam JWT, pois ele não está implementado.
- Não foi realizado novo cadastro no banco real, teste manual no Thunder Client ou verificação de implantação Azure nesta avaliação.

## Ordem sugerida para adequação

1. Adicionar JJWT e configuração de chave e expiração via ambiente.
2. Criar Perfil USER/ADMIN persistido e planejar a atribuição de USER aos usuários existentes.
3. Separar os DTOs e o AuthService, preservando BCrypt e validações já presentes.
4. Implementar JwtService e JwtAuthenticationFilter, integrados ao SecurityConfig.
5. Definir o contrato da aula: cadastro JSON, login JSON com token Bearer e usuário, rota privada e /me autenticado por JWT.
6. Adaptar o cliente Vue para usar o contrato JWT e revisar o arquivo Api.js que hoje está desconectado do fluxo.
7. Adicionar testes de JWT válido, ausente, inválido e expirado; permissões USER/ADMIN; e os quatro cenários do Thunder Client.
8. Atualizar README, .env.example e scripts de execução e gerar um novo JAR antes de reiniciar a API.

A implementação atual pode ser reaproveitada. A principal pendência é implementar o mecanismo JWT de ponta a ponta e alinhar os contratos HTTP à apostila.
