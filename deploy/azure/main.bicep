targetScope = 'resourceGroup'
@description('Application name')
param appName string = 'cti-insights'
param location string = resourceGroup().location
@description('Existing Container Apps environment resource ID')
param environmentId string
@description('Public frontend image with immutable tag')
param frontendImage string
@description('Public backend image with immutable tag')
param backendImage string
@description('PostgreSQL JDBC URL with sslmode=require')
param databaseUrl string
param databaseUser string
@secure()
param databasePassword string
@secure()
param jwtSecretBase64 string
resource app 'Microsoft.App/containerApps@2025-01-01' = {
  name: appName
  location: location
  properties: {
    environmentId: environmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        targetPort: 80
        allowInsecure: false
        transport: 'auto'
      }
      secrets: [
        {
          name: 'jwt-secret'
          value: jwtSecretBase64
        }
        {
          name: 'database-password'
          value: databasePassword
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'frontend'
          image: frontendImage
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            {
              name: 'API_UPSTREAM'
              value: 'http://127.0.0.1:8080'
            }
          ]
        }
        {
          name: 'backend'
          image: backendImage
          resources: {
            cpu: json('0.75')
            memory: '1.5Gi'
          }
          env: [
            { name: 'PORT', value: '8080' }
            { name: 'DATABASE_URL', value: databaseUrl }
            { name: 'DATABASE_USER', value: databaseUser }
            { name: 'DATABASE_PASSWORD', secretRef: 'database-password' }
            { name: 'PYTHON_EXECUTABLE', value: 'python3' }
            { name: 'PYTHON_SCRIPT', value: '/app/python/analyze.py' }
            { name: 'JWT_SECRET_BASE64', secretRef: 'jwt-secret' }
            { name: 'JWT_EXPIRATION_SECONDS', value: '1800' }
            { name: 'DEBUG', value: 'false' }
          ]
          probes: [
            {
              type: 'Startup'
              httpGet: {
                path: '/actuator/health'
                port: 8080
              }
              initialDelaySeconds: 10
              periodSeconds: 10
              failureThreshold: 30
            }
          ]
        }
      ]
      // Uma replica inicial; todas as replicas devem compartilhar o segredo JWT.
      scale: {
        minReplicas: 1
        maxReplicas: 1
      }
    }
  }
}
output url string = 'https://${app.properties.configuration.ingress.fqdn}'
