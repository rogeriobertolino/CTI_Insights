-- Consultas para o schema deste projeto. Troque lote_id = 1 pelo lote desejado.
SELECT id, status, total_linhas, linhas_validas, linhas_invalidas, mensagem
FROM upload_lote ORDER BY id DESC;

SELECT COUNT(*) AS clientes, COUNT(DISTINCT id_consultor) AS consultores
FROM cliente WHERE lote_id = 1;

SELECT COUNT(*) AS contratos, COUNT(DISTINCT id_servico) AS servicos
FROM contrato WHERE lote_id = 1;

SELECT lote_id, criado_em, LENGTH(resultado) AS tamanho,
       resultado::json -> 'resumo' ->> 'totalClientes' AS clientes,
       resultado::json -> 'resumo' ->> 'media' AS media,
       resultado::json -> 'resumo' ->> 'mediana' AS mediana,
       resultado::json -> 'resumo' ->> 'desvioPadrao' AS desvio_padrao
FROM estatistica_lote WHERE lote_id = 1;
