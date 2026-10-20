# Revisão comparativa do backend de referência

Data: 19/10/2026.

Referência: C:/Users/Rogerio Bertolino/OneDrive/Documentos/Des_Backend/ctiinsigths/ctiinsights/src
Projeto comparado: CTI_Insights/backend.

## Parecer

Há padrões e funcionalidades que podem melhorar o trabalho. O projeto de referência desenvolve mais CRUDs do domínio comercial; o CTI_Insights já possui autenticação JWT, BCrypt, perfis, aprovação administrativa e DTOs que devem ser preservados.

A proposta é adaptar os módulos úteis aos contratos e modelos atuais. Copiar toda a pasta substituiria modelos diferentes, introduziria um login de consultor sem hash e poderia deixar novos endpoints fora das regras de segurança.

Foram revisados todos os 32 arquivos de src: 1 inicializador, 8 controllers, 8 modelos/enums, 6 repositories, 6 services, 1 configuração e 2 arquivos de testes. O pom.xml também foi conferido como contexto.

## Achados prioritários na referência

### Alta: senha salva sem hash e exposta na resposta direta

- service/ConsultorService.java: criar salva a entidade recebida; login compara getSenha().equals(senha); atualizar atribui a senha recebida.
- models/Consultor.java: há getter público getSenha(), sem exclusão da serialização, e unique=true na coluna senha.
- controller/ConsultorController.java: cadastro, login, consulta e listagem retornam Consultor diretamente.
- Consequência: a serialização direta de uma conta pode devolver a senha; comparar texto não implementa BCrypt.
- O teste já existente sobre serialização cobre Servico e Insights, não o retorno direto de Consultor.
- Preservar Usuario/AuthService/DTOs/BCrypt do CTI_Insights. Consultor deve continuar sendo uma entidade comercial, vinculável a Usuario se houver necessidade, sem um segundo login em texto puro.

### Alta: não transportar as rotas sem adaptar a segurança

Os controllers da referência usam /consultores, /contratos, /servicos, /insights e /telemetrias. Na configuração atual do CTI_Insights, a exigência de autenticação é aplicada às rotas /api/**; a regra final permite outras requisições.

Se esses controllers forem copiados com os caminhos originais, os novos endpoints ficarão fora dessa proteção. Isso é um risco da integração proposta, não uma afirmação de que esses endpoints já existam desprotegidos no CTI_Insights. Novos recursos devem ficar sob /api e ter testes de 401/403.

### Alta: configuração de banco com credenciais literais

application.properties contém URL, usuário e senha como valores literais. Eles foram lidos apenas para classificar a configuração e não são reproduzidos neste relatório. A cópia usada nos testes teve essa configuração substituída por um nome de aplicação, sem conexão ao banco.

Manter variáveis de ambiente e o armazenamento local protegido do CTI_Insights.

### Média: dependência da camada de serviço para controller

service/ClienteService.java importa e recebe ConsultorController no construtor, mantendo um campo que não é utilizado. Serviços devem depender de regras e repositories necessários ao caso de uso; remover a dependência do controller ao adaptar o código.

### Média: entidades expostas, relacionamentos e exclusões

Os controllers recebem/devolvem entidades JPA. Há relações bidirecionais Consultor/Cliente/Contrato e Cliente.contratos com CascadeType.ALL. As anotações JsonIgnoreProperties em alguns vínculos evitam parte dos ciclos, mas não substituem DTOs nem uma política de exclusão.

A exclusão em cascata pode alcançar contratos quando o cliente for removido por JPA. Definir se contratos devem bloquear exclusão, ser encerrados ou ser removidos; não transportar essa política implicitamente.

### Média: tipos e regras inconsistentes

- Contrato.valor e Servico.preco usam Double; manter BigDecimal para os valores monetários no modelo atual.
- Insights.data é String; Telemetria combina dataHora com uma segunda data String. Escolher tipos temporais e significado de cada data.
- Cliente.email é obrigatório no mapeamento, porém o construtor não o recebe; o serviço não valida todos os campos obrigatórios antes de salvar.
- Cadastro e atualização precisam das mesmas regras de campos e formatos.
- InsightsService resolve cliente e contrato separadamente; quando ambos são fornecidos, também é necessário verificar a coerência entre eles.
- Algumas buscas lançam RuntimeException, outras ResponseStatusException: padronizar erros HTTP.

## Comparação funcional

| Área | Referência | CTI_Insights atual | Decisão proposta |
| --- | --- | --- | --- |
| Autenticação | Login no Consultor, sem JWT/Security no POM | JWT, BCrypt, Usuario separado | Manter o mecanismo atual |
| Aprovação administrativa | Não localizada | PENDENTE/APROVADO/RECUSADO e página ADMIN | Manter |
| Clientes | Criar, buscar e atualizar; email no modelo | CRUD com DTOs, código CTI único e importação | Reforçar validação, paginação e decidir persistência do email |
| Consultores | CRUD e matrícula | Entidade e repository, criados/reutilizados na importação | Criar módulo CRUD com DTOs; matrícula é opcional de negócio |
| Serviços | CRUD e preço | Entidade com nome/descrição/categoria, sem API CRUD | Completar módulo e decidir se haverá preço |
| Contratos | CRUD e resolução de cliente/serviço | Entidade com dinheiro decimal, datas e status, sem API CRUD | Criar serviço/repository/DTO/controller sobre o modelo atual |
| Insights | CRUD manual | Entidade Insight; dashboard calculado pelo Python, sem API CRUD dessa entidade | Definir consulta/geração; não confundir dashboard com persistência de Insight |
| Telemetria | CRUD manual | Entidade Telemetria sem repository/service de captura | Registrar eventos reais de upload/processamento e consultar com paginação |
| Enum de nível/status | Dois endpoints | Enums existem, sem endpoints próprios | Disponibilizar sob /api se forem consumidos pelos formulários |
| Testes | 8 unitários e 1 contexto | Suítes JWT, aprovação e importação | Somar testes de negócio, sem substituir os testes atuais |

## Melhorias concretas no nosso projeto

1. **Respostas 404:** ClienteService.find e consultor lançam IllegalArgumentException. ApiExceptionHandler converte essa exceção para 400. Para IDs inexistentes, usar uma exceção específica ou ResponseStatusException com 404 e testar o contrato.
2. **Validação com limites:** ClienteRequest valida presença e faturamento, mas não limita os textos aos tamanhos das colunas de Cliente. Adicionar limites de 80 para codigoCti, 180 para nomeEmpresa e 100 para segmento, além de IDs positivos. Isso evita que entradas inválidas dependam do erro de banco.
3. **Paginação e consultas:** ClienteService.list carrega todos os clientes; planejar um endpoint paginado e busca por consultor/segmento/nível. Preservar contratos atuais ou oferecer compatibilidade ao mudar a resposta.
4. **Email de cliente:** o email é lido/validado na planilha e pode permanecer nos registros JSON importados, mas não há campo email no Cliente persistido do nosso domínio. Definir se será contato do cliente e como migrar os dados. Não copiar a obrigatoriedade/unicidade sem regra de negócio.
5. **Completar os módulos comerciais:** Consultor, Servico, Contrato, Insight e Telemetria não possuem no projeto atual todo o conjunto CRUD/captura existente ou sugerido na referência. Entidade anotada não significa funcionalidade pronta.
6. **Legibilidade:** AuthService, JwtAuthenticationFilter, CadastroAdminService e ImportService têm muitos comandos na mesma linha e imports amplos/nomes totalmente qualificados. Formatar métodos, padronizar nomes e separar blocos para facilitar revisão.
7. **Erros e logs:** ApiExceptionHandler devolve formatos diferentes entre handlers; unificar um DTO de erro. Registrar causas internas no servidor sem expor senhas/tokens/dados pessoais nas respostas.
8. **Auditoria da importação:** ImportService marca um lote como falho dentro da mesma transação que reverte em Exception. Se o requisito for manter histórico de falhas, será necessário registrar esse evento em uma transação/fluxo separado; não mudar a atomicidade sem decidir a regra.
9. **Migrações de esquema:** o ambiente atual usa ddl-auto=update. Antes de compartilhar/publicar uma base estável, versionar alterações de esquema e testar a atualização de bases existentes. Preservar os usuários e aprovações já configurados.

## Padrão sugerido para o trabalho

Organizar por funcionalidade e manter as responsabilidades reconhecíveis dentro de cada módulo:

- auth: autenticação, usuários, perfis e aprovação.
- clients: cliente, DTOs, controller, service e repository.
- consultants: cadastro e consulta de consultores.
- services: catálogo de serviços.
- contracts: contratos e vínculos comerciais.
- insights: geração/consulta de análises persistidas.
- telemetry: eventos reais e consulta operacional.
- imports: leitura, validação e coordenação de importações.
- shared: tratamento comum de erros e utilitários compartilhados.

Regras: controller recebe DTO validado; service executa o caso de uso em transação; repository acessa os dados; entidade protege invariantes; DTO de saída define o que o cliente recebe. Usar injeção por construtor, respostas HTTP consistentes, dinheiro decimal e datas tipadas. Nomes de pastas por camada na raiz são uma opção didática; nosso agrupamento por funcionalidade pode atender às mesmas responsabilidades.

## Compatibilidade antes de integrar

- A referência usa Spring Boot 4.1.1 e Java 25; nosso backend usa Spring Boot 3.5.16 e Java 25.
- A referência usa JsonMapper de tools.jackson nos testes; nosso projeto utiliza com.fasterxml.jackson. Não transportar imports/dependências sem adaptação.
- Há tabelas diferentes: contratos/contrato, servicos/servico, insights/insight e telemetrias/telemetria.
- Nosso Cliente usa codigoCti único; não remover esse identificador para adotar o email da referência.
- Nosso Contrato exige datas, valor decimal e status; não substituí-lo pelo modelo reduzido da referência.
- As rotas atuais /api/clientes e /api/v1 de importação já têm consumidores. Uma padronização de versão deve manter compatibilidade ou atualizar explicitamente os consumidores.

## Ordem de trabalho proposta

1. Corrigir os contratos 404, completar as validações e formatar os arquivos atuais.
2. Criar CRUD de consultores e serviços usando DTOs e testes.
3. Criar contratos, validando cliente/serviço, datas, valor e exclusão.
4. Integrar telemetria ao fluxo real de upload, mantendo auditoria de falhas.
5. Definir como os insights serão gerados/persistidos e criar suas consultas.
6. Integrar as telas e medir consultas antes de otimizar índices/cargas.

Cada etapa deve preservar JWT, aprovação administrativa e dados existentes, e deve ter testes focados no comportamento alterado.

## Validação desta revisão

- Leitura de todos os arquivos de src e do POM.
- Oito testes ControllersServicesTests executados em uma cópia isolada no workspace; resultado registrado abaixo.
- A configuração de conexão foi removida da cópia antes da execução. Não foi executado contextLoads nem teste contra PostgreSQL da referência.
- Verificação adicional de serialização executada somente na cópia, com senha fictícia; resultado registrado abaixo.
- Nenhum arquivo da pasta original ou código de produção foi alterado. O parecer é uma avaliação e uma proposta de implementação.


Resultados: os 8 testes fornecidos passaram; a verificação adicional também passou, confirmando que o JSON de Consultor contém a senha fictícia. Isso confirma o achado e não significa que esse comportamento seja desejável. As 31 fontes Java da cópia foram comparadas por SHA-256 com os originais e coincidem.

## Inventário de todos os arquivos revisados

| Arquivo relativo a src | Avaliação |
| --- | --- |
| main/java/com/ctiinsights/ctiinsights/controller/ClienteController.java | Separação controller/service; recebe/devolve entidade. Faltam listagem/exclusão e validação de DTO; POST sem 201 explícito. |
| main/java/com/ctiinsights/ctiinsights/controller/ConsultorController.java | CRUD e login de consultor. Retorno direto pode expor senha e coleções; preservar a autenticação separada do projeto atual. |
| main/java/com/ctiinsights/ctiinsights/controller/ContratoController.java | CRUD com 201 e 204 explícitos. Reaproveitar contrato HTTP, adaptando DTOs e rotas protegidas. |
| main/java/com/ctiinsights/ctiinsights/controller/InsightsController.java | CRUD de insights; adaptar para a entidade Insight e DTOs existentes/novos. |
| main/java/com/ctiinsights/ctiinsights/controller/NivelClienteController.java | Endpoint simples para valores do enum; pode orientar opções de formulários. |
| main/java/com/ctiinsights/ctiinsights/controller/ServicoController.java | CRUD com respostas HTTP explícitas; funcionalidade útil para ampliar nosso domínio de serviços. |
| main/java/com/ctiinsights/ctiinsights/controller/StatusClienteController.java | Endpoint de valores do enum; útil com rota sob /api e acesso definido. |
| main/java/com/ctiinsights/ctiinsights/controller/TelemetriaController.java | CRUD manual de telemetria; diferenciar de captura automática de eventos de upload. |
| main/java/com/ctiinsights/ctiinsights/CtiinsightsApplication.java | Inicialização padrão do Spring Boot; equivalente funcional ao ponto de entrada atual. |
| main/java/com/ctiinsights/ctiinsights/models/Cliente.java | BigDecimal e enums adequados. Email obrigatório não está no construtor; coleções bidirecionais e cascade ALL exigem política de exclusão. |
| main/java/com/ctiinsights/ctiinsights/models/Consultor.java | Matrícula pode ser útil. Senha pública por getter, persistida sem hash pelo serviço e marcada unique: não incorporar esse modelo de autenticação. |
| main/java/com/ctiinsights/ctiinsights/models/Contrato.java | Relacionamentos mapeados; dinheiro em Double e tabela contratos diferem do modelo atual com BigDecimal, datas e status. |
| main/java/com/ctiinsights/ctiinsights/models/Insights.java | Relações com cliente e contrato; data como String deve ser tipada. Nome/tabela diferem de Insight atual. |
| main/java/com/ctiinsights/ctiinsights/models/NivelCliente.java | Enum A/B/C equivalente ao atual. |
| main/java/com/ctiinsights/ctiinsights/models/Servico.java | CRUD utiliza preço, ausente no modelo atual. Adaptar com BigDecimal se essa informação fizer parte do negócio. |
| main/java/com/ctiinsights/ctiinsights/models/StatusCliente.java | Enum ATIVO/INATIVO equivalente ao atual. |
| main/java/com/ctiinsights/ctiinsights/models/Telemetria.java | Evento/origem/mensagem são úteis, mas há dataHora e data String duplicando o conceito; sem vínculo com consultor no modelo de referência. |
| main/java/com/ctiinsights/ctiinsights/repository/ClienteRepository.java | JpaRepository básico; adicionar paginação/consultas e constraints conforme o código CTI do projeto atual. |
| main/java/com/ctiinsights/ctiinsights/repository/ConsultorRepository.java | Busca por email; reaproveitar a intenção, mantendo normalização e busca sem diferença de caixa do projeto atual. |
| main/java/com/ctiinsights/ctiinsights/repository/ContratoRepository.java | Repository básico; ainda ausente como módulo funcional no projeto atual. |
| main/java/com/ctiinsights/ctiinsights/repository/InsightsRepository.java | Repository básico; adaptar nome para Insight e sua tabela atual. |
| main/java/com/ctiinsights/ctiinsights/repository/ServicoRepository.java | Repository básico; útil ao completar CRUD de serviços. |
| main/java/com/ctiinsights/ctiinsights/repository/TelemetriaRepository.java | Repository básico; útil para persistir eventos reais, além da consulta paginada. |
| main/java/com/ctiinsights/ctiinsights/service/ClienteService.java | Usa transação e resolve consultor; injeta ConsultorController sem utilizá-lo. Validações de criação/atualização incompletas. |
| main/java/com/ctiinsights/ctiinsights/service/ConsultorService.java | Valida duplicidade de email; senha é salva como recebida e comparada por equals. Atualização não repete todas as validações. |
| main/java/com/ctiinsights/ctiinsights/service/ContratoService.java | Boa resolução de relacionamentos e 404; permite associações nulas e não valida todos os campos. Adaptar requisitos do modelo atual. |
| main/java/com/ctiinsights/ctiinsights/service/InsightsService.java | Boa resolução de referências; falta verificar se contrato e cliente pertencem à mesma relação quando ambos são enviados. |
| main/java/com/ctiinsights/ctiinsights/service/ServicoService.java | Transações e nome obrigatório; faltam validação monetária e paginação. |
| main/java/com/ctiinsights/ctiinsights/service/TelemetriaService.java | CRUD e preservação do ID da URL; não integra automaticamente os eventos do fluxo de upload. |
| main/resources/application.properties | Configuração PostgreSQL e ddl-auto=update; credenciais estão como valores literais. Valores sensíveis não foram reproduzidos. |
| test/java/com/ctiinsights/ctiinsights/ControllersServicesTests.java | Oito testes unitários úteis, todos aprovados. Cobre relacionamentos, enums e serialização de serviço/insights; não cobre a exposição direta de Consultor. |
| test/java/com/ctiinsights/ctiinsights/CtiinsightsApplicationTests.java | Teste de contexto depende da configuração de banco; não executado nesta revisão. |


## Implementação posterior

As lacunas identificadas nesta revisão foram tratadas no projeto atual. Consulte [Correcoes_Projeto.md](Correcoes_Projeto.md) para alterações, migração, testes e endereço ativo. A análise acima registra a situação encontrada originalmente.
