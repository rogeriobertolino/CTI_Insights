# Automação Industrial — integração TI/TA

O CTI Insights trata dados comerciais. Não possui sensores, atuadores, robôs ou CLP. Os tópicos de aquisição de sinais físicos e envio de comandos ao CLP não são implementados neste MVP. A aplicação à disciplina está na integração entre sistemas, coleta, persistência e visualização.

## Comunicação implementada

O navegador envia uma planilha por HTTP multipart para `POST /api/v1/imports`, com JWT de usuário aprovado. Spring Boot executa o ETL Python e recebe JSON pelo processo local, valida o resultado e grava no PostgreSQL. O dashboard consulta JSON pela API. Em publicação, o tráfego web deve utilizar HTTPS; a conexão PostgreSQL utiliza seu próprio protocolo com TLS, não HTTP.

| Aspecto | HTTP/REST do projeto | Modbus TCP, referência teórica |
|---|---|---|
| Objetivo | Trocar arquivos, recursos e análises comerciais | Ler e escrever dados de dispositivos industriais |
| Organização | Recursos e verbos HTTP; JSON e multipart | Funções sobre coils e registradores |
| Comunicação | Cliente web faz requisição à API | Cliente consulta servidor/dispositivo |
| Porta usual | HTTPS 443; desenvolvimento conforme configuração | TCP 502 |
| Segurança | HTTPS e autorização JWT na aplicação | Modbus TCP clássico demanda proteção de rede; não equivale a HTTPS/JWT |
| Implementação no CTI | Implementada | Não implementada; comparação acadêmica |

A comparação se apoia na [especificação oficial Modbus](https://www.modbus.org/file/secure/modbusprotocolspecification.pdf). Existem extensões de segurança; a comparação acima se refere ao Modbus TCP clássico.

## Coleta e monitoramento

A planilha é uma coleta em lote produzida por pessoas. Ela não mede variáveis físicas. A validação identifica problemas de qualidade; a transformação padroniza a cópia de trabalho e mantém as ocorrências para revisão.

Os gráficos são atualizados após a conclusão da importação ou a consulta de um lote. Isso é atualização por evento de processamento, sem aquisição contínua, streaming ou promessa de tempo real industrial. O dashboard monitora a carteira comercial e pode apoiar planejamento de serviços e distribuição de clientes.

## Evidência para apresentação

Demonstrar uma planilha fictícia com inconsistências, sua prévia e os dados tratados; importar, consultar o lote e explicar segmento, nível e faturamento. Registrar as capturas de tela de desktop e celular e a data da execução na matriz de evidências. Uma captura não comprova comunicação com CLP; essa funcionalidade está fora do escopo.
