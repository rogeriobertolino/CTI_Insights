# Organização do CTI Insights

Revisão de 22/10/2026. Referência: Aula 10 de Desenvolvimento Backend, páginas 3, 14 e 15–23.

## Pastas e responsabilidades

```text
CTI_Insights/
├── src/                         # Frontend Vue
│   ├── assets/styles/           # Estilos globais e validação
│   ├── components/              # Componentes reutilizáveis
│   ├── layouts/                 # Estrutura visual das páginas
│   ├── router/                  # Rotas e controle de navegação
│   ├── services/                # Clientes HTTP e integração com a API
│   ├── stores/                  # Estado compartilhado e stores Pinia
│   ├── utils/                   # Conversões, exportações e validações auxiliares
│   ├── views/                   # Páginas
│   ├── App.vue
│   └── main.js
├── backend/
│   ├── src/main/java/com/cti/provedor/
│   │   ├── config/              # SecurityConfig
│   │   ├── controller/          # Endpoints HTTP e tratamento de erros
│   │   ├── dto/                 # Entrada e saída da API
│   │   ├── model/               # Entidades JPA e enums
│   │   ├── repository/          # Acesso ao banco via Spring Data JPA
│   │   ├── security/            # JwtService e JwtAuthenticationFilter
│   │   ├── service/             # Regras de negócio e processamento
│   │   └── CtiProvedorApplication.java
│   ├── src/main/resources/application.properties
│   ├── src/test/                # Testes Java e configuração H2
│   ├── Dockerfile
│   └── pom.xml
├── python/                      # Entrada do processamento chamado pelo Spring
├── analytics-python/            # Limpeza, estatística, gráficos e insights
├── tests/
│   ├── frontend/                # Testes Node do frontend
│   ├── python/                  # Testes da entrada de processamento
│   └── analytics/               # Testes do módulo científico
├── scripts/
│   └── docs/                    # Gerador do relatório acadêmico
├── docs/
│   ├── exemplos/               # Exemplos didáticos
│   └── reports/                # Relatórios técnicos, incluindo dependências Java
└── deploy/
    ├── azure/                   # Infraestrutura Azure
    └── migrations/              # Migrações SQL
```

Os arquivos de entrada do Vite, npm, Docker e hospedagem permanecem na raiz,
onde as ferramentas os procuram. Os testes Java seguem a convenção Maven.
Os dois diretórios Python têm responsabilidades diferentes e continuam nos
caminhos usados pelos contêineres e pela API.

## Relação com o material da aula

A página 3 apresenta as seis camadas gerais. A página 14 detalha a pasta
`security/`, também explicada na página 21. Por isso, `config/` contém
`SecurityConfig` e `security/` contém a implementação JWT. O pacote do projeto
permanece `com.cti.provedor`; o nome `br.senai.aula09` é o pacote do exemplo.

O projeto tem Web, JPA, PostgreSQL, Security, Validation e JJWT no POM;
entidade `Usuario`, enum `Perfil`, DTOs de cadastro/login/resposta, BCrypt,
`AuthService`, filtro JWT, rotas autenticadas e configuração por ambiente.
A conexão real com Azure não foi validada nesta revisão de organização.

A avaliação de 19/10 em `Avaliacao_Aula_10.md` registra o estado anterior à
implementação JWT. Para a estrutura atual, use este documento; para o
funcionamento da autenticação, use `Autenticacao_JWT_Aula_10.md`.

## Verificação local

Na raiz, com as dependências instaladas:

```powershell
npm test
npm run build
python -m unittest discover -s tests/python
python -m unittest discover -s tests/analytics
mvn -f backend/pom.xml test
```

Os testes científicos exigem as dependências de `analytics-python/requirements.txt`.
Use o ambiente Python científico configurado no computador. O Maven exige JDK 25.
O teste Java com Python real é opcional e requer `CTI_FULL_PIPELINE=true` e
`CTI_TEST_PYTHON` apontando para o executável Python com as dependências.

`node_modules/`, `.venv/`, `.runtime/`, `dist/`, `backend/target/`, `tmp/` e
`output/` guardam dependências ou arquivos gerados. Não são pastas de código-fonte.

## Resultado da verificação

Em 22/10/2026:

- Frontend: 52 testes aprovados e build Vite concluído.
- Processamento Python: 8 testes aprovados.
- Análise científica: 5 testes aprovados, incluindo exportação dos gráficos.
- Backend: 26 testes aprovados, sem testes ignorados, com Python real e H2.
- Maven `verify`: concluído com geração do JAR em `.runtime/organization-build/`.
- Pacotes e diretórios: correspondência conferida nos 85 arquivos Java.
- `git diff --check`: sem problemas.

A revisão corrigiu o agrupamento de gráficos para registros sem segmento,
atualizou a asserção do teste integrado para o campo normalizado `cliente` e
sincronizou as dependências já declaradas no ambiente Python científico local.
O build do frontend mantém um aviso de tamanho de bundle acima de 500 kB;
esse aviso não impede a compilação.
