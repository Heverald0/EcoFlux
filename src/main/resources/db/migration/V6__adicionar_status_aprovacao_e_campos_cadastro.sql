ALTER TABLE tb_usuarios
    ADD COLUMN documento VARCHAR(20) NULL UNIQUE AFTER email,
    ADD COLUMN telefone VARCHAR(20) NULL AFTER documento,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ATIVO' AFTER perfil,
    ADD COLUMN motivo_rejeicao VARCHAR(255) NULL AFTER status;

ALTER TABLE tb_instituicoes
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'AGUARDANDO_ANALISE' AFTER tipo,
    ADD COLUMN motivo_rejeicao VARCHAR(255) NULL AFTER status;

UPDATE tb_instituicoes SET status = 'ATIVA';

CREATE INDEX idx_usuarios_status ON tb_usuarios(status);
CREATE INDEX idx_instituicoes_status ON tb_instituicoes(status);
CREATE INDEX idx_instituicoes_tipo ON tb_instituicoes(tipo);