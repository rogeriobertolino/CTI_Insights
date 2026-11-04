# Validação da complementação — 29/10/2026

A cobertura funcional do material recebido foi integrada ao projeto local. O contrato implementado e as decisões de adaptação estão em [Contrato da carteira anual](Contrato_Carteira_Anual.md). O relatório comparativo original registra a situação anterior à implementação.

## Verificações concluídas

| Área | Resultado |
| --- | --- |
| Frontend | Compilação de produção concluída; 54 testes aprovados |
| Python | 8 testes de análise/exportação e 8 testes de leitura/estatística aprovados |
| Java | 27 testes de autenticação, administração e domínio comercial aprovados |
| Integração real | 2 testes com Python real e banco H2 isolado aprovados; inclui os formatos mensal e anual |
| Amostra do RAR | 38 registros; mesmas 32 linhas válidas da referência e 6 rejeitadas |
| Persistência da amostra | 32 clientes, 4 consultores, 6 serviços e 61 contratos; reenvio não duplica a carteira |
| Faturamento anual | Média R$ 2.261.250,00; mediana R$ 1.470.000,00 |
| Contratos sem preço na origem | Persistidos como PENDENTE com valor não informado, sem inventar preço zero |
| Exportações | CSVs, JSON, insights e quatro gráficos PNG produzidos no teste |
| Dados acadêmicos e acesso | Matrícula/turma persistidos; bloqueio e reaprovação testados |
| Migração local | Aplicada com backup prévio do banco; sem substituir tabelas ou importar a amostra no PostgreSQL local |

São **99 testes distintos aprovados** nas suítes acima. As duas integrações usam banco temporário de teste, separado dos dados do usuário. O ambiente científico local usa o Python 3.12 externo à pasta sincronizada, previsto no script `scripts/setup-analytics.ps1`.

## Arquivos para uso

- [Planilha original, com abas didáticas e dicionário](../sample-data/dados.xlsx).
- Tela de upload: oferece o modelo mensal existente e o novo modelo anual.
- Relatório da planilha: exibe erros bloqueantes e avisos de padronização separados.
- Dashboard: exibe período correto, ranking, serviços, consultores, correlações e geografia.
- Administração: matrícula/turma e ações de bloqueio/reaprovação.

As alterações de código, testes, documentação e migração estão no projeto local. Nenhum recurso Azure foi provisionado nesta etapa.
