ALTER TABLE tb_usuarios ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE tb_usuarios ADD COLUMN versao_token BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_usuarios_status_perfil ON tb_usuarios(status, perfil);
