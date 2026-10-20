-- Evolução aditiva para bases CTI existentes. Executar após backup, antes do novo backend.
-- As tabelas originais e os usuários são preservados. Uma base nova usa os mapeamentos JPA iniciais.
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS email varchar(254);
ALTER TABLE consultor ADD COLUMN IF NOT EXISTS matricula varchar(50);
ALTER TABLE servico ADD COLUMN IF NOT EXISTS preco numeric(15,2);
ALTER TABLE telemetria ALTER COLUMN id_consultor DROP NOT NULL;
ALTER TABLE telemetria ADD COLUMN IF NOT EXISTS usuario varchar(254);
CREATE INDEX IF NOT EXISTS idx_telemetria_data_hora ON telemetria(data_hora);
