-- Aditiva: preserva dados e contratos existentes.
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS matricula varchar(30);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS turma varchar(60);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS cidade varchar(120);
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS uf varchar(2);
-- Preço desconhecido não equivale a preço zero.
ALTER TABLE contrato ALTER COLUMN valor DROP NOT NULL;
-- Hibernate normalmente gera esse CHECK ao mapear um enum String.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_status_cadastro_check;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_status_cadastro_check
  CHECK (status_cadastro IN ('PENDENTE','APROVADO','RECUSADO','BLOQUEADO'));
