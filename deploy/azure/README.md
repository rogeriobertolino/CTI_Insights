# CTI Insights no Azure

Preparação para Azure Container Apps com frontend Nginx e backend Java/Python na mesma aplicação. O PostgreSQL é externo e persistente. O template não cria nem apaga o banco.

## Dependências de publicação

- Conta e assinatura Azure, grupo de recursos e ambiente Container Apps.
- PostgreSQL já preparado e acessível pelo ambiente, com JDBC `jdbc:postgresql://HOST:5432/cti_insights?sslmode=require`.
- Imagens de frontend e backend publicadas com tag imutável em registro público. Para registro privado, configurar identidade gerenciada/AcrPull e registries antes da publicação.
- Azure CLI com Bicep e Docker Desktop em execução.
- Chave JWT Base64 aleatória de pelo menos 32 bytes, fornecida no parâmetro seguro jwtSecretBase64.

## Construir imagens

```powershell
docker build -t REGISTRO/cti-frontend:TAG .
docker build -f backend/Dockerfile -t REGISTRO/cti-backend:TAG .
```

Publique as imagens no registro escolhido. Compile o template com `az bicep build --file deploy/azure/main.bicep`. Em seguida, use `az deployment group what-if` com o mesmo template e os parâmetros do ambiente. Forneça a senha por entrada segura ou arquivo temporário fora do Git; não escreva segredos em comandos salvos no repositório. Revise o resultado antes de executar `az deployment group create`.

O ambiente e o PostgreSQL não foram provisionados por este trabalho. Não foi realizada publicação. Custos e permissões dependem da assinatura escolhida.

## Verificação depois de publicar

1. Abra a URL HTTPS retornada pelo template.
2. Cadastre um usuário e faça login.
3. Importe a planilha, revise as ocorrências e envie os registros válidos.
4. Confira o dashboard, encerre a sessão e confirme que a API exige login.
5. Reinicie a revisão e confirme que usuários e análises continuam no PostgreSQL. JWTs continuam válidos até expirar quando a chave e o emissor são mantidos.

A API fica acessível pelo proxy /api da mesma origem. A autenticação usa JWT Bearer, com chave fornecida como secretRef. O navegador mantém o JWT no sessionStorage; sair remove a cópia local, e tokens emitidos continuam válidos até expirar. O template mantém uma réplica inicial; outras réplicas devem compartilhar chave e emissor. Todos os usuários autenticados participam da mesma carteira comercial: não há isolamento por empresa/conta.

Referência: https://learn.microsoft.com/en-us/azure/templates/microsoft.app/2025-01-01/containerapps
