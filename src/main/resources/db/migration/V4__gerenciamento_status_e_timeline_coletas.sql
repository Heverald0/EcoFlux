ALTER TABLE tb_coletas
    ADD COLUMN janela_horario VARCHAR(50) DEFAULT 'COMERCIAL (08:00 - 18:00)',
    ADD COLUMN cooperativa_id BIGINT NULL,
    ADD CONSTRAINT fk_coleta_cooperativa FOREIGN KEY (cooperativa_id) REFERENCES tb_instituicoes(id);

CREATE TABLE tb_coletas_historico_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coleta_id BIGINT NOT NULL,
    status_anterior VARCHAR(50),
    status_novo VARCHAR(50) NOT NULL,
    observacao VARCHAR(255),
    usuario_autor_id BIGINT NOT NULL,
    data_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_coleta FOREIGN KEY (coleta_id) REFERENCES tb_coletas(id) ON DELETE CASCADE,
    CONSTRAINT fk_historico_autor FOREIGN KEY (usuario_autor_id) REFERENCES tb_usuarios(id)
);

CREATE INDEX idx_historico_coleta_id ON tb_coletas_historico_status(coleta_id);