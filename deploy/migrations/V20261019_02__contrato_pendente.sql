ALTER TABLE contrato DROP CONSTRAINT IF EXISTS contrato_status_check;
ALTER TABLE contrato ADD CONSTRAINT contrato_status_check CHECK (status IN ('ATIVO','PENDENTE','ENCERRADO','CANCELADO'));
