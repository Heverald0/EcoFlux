ALTER TABLE tb_usuarios ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE tb_usuarios ADD COLUMN versao_token BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tb_instituicoes ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE tb_instituicoes ALTER COLUMN status SET DEFAULT 'PENDENTE';
ALTER TABLE tb_instituicoes ADD COLUMN motivo_status VARCHAR(500);
ALTER TABLE tb_instituicoes ADD COLUMN data_analise TIMESTAMP NULL;
ALTER TABLE tb_instituicoes ADD COLUMN administrador_analise_id BIGINT;
ALTER TABLE tb_instituicoes ADD CONSTRAINT fk_instituicao_analise FOREIGN KEY (administrador_analise_id) REFERENCES tb_usuarios(id);
CREATE INDEX idx_instituicoes_status ON tb_instituicoes(status);
CREATE INDEX idx_usuarios_status_perfil ON tb_usuarios(status, perfil);
UPDATE tb_usuarios SET perfil = 'COOPERATIVA'
WHERE perfil = 'INSTITUICAO' AND id IN (
    SELECT usuario_responsavel_id FROM tb_instituicoes WHERE tipo = 'COOPERATIVA'
);
