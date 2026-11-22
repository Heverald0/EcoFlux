ALTER TABLE tb_usuarios ADD COLUMN documento VARCHAR(20) NULL UNIQUE;
ALTER TABLE tb_usuarios ADD COLUMN telefone VARCHAR(20) NULL;
ALTER TABLE tb_usuarios ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE tb_usuarios ADD COLUMN motivo_rejeicao VARCHAR(255) NULL;

ALTER TABLE tb_instituicoes ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_ANALISE';
ALTER TABLE tb_instituicoes ADD COLUMN motivo_rejeicao VARCHAR(255) NULL;

UPDATE tb_instituicoes SET status = 'ATIVA';

CREATE INDEX idx_usuarios_status ON tb_usuarios(status);
CREATE INDEX idx_instituicoes_status ON tb_instituicoes(status);
CREATE INDEX idx_instituicoes_tipo ON tb_instituicoes(tipo);