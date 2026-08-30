CREATE TABLE tb_coletas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_hora_agendamento TIMESTAMP NOT NULL,
    tipo_material VARCHAR(50) NOT NULL,
    quantidade_estimada_kg DECIMAL(10, 2) NOT NULL,
    observacoes VARCHAR(255),
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    cep VARCHAR(10) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AGENDADA',
    usuario_solicitante_id BIGINT NOT NULL,
    instituicao_id BIGINT,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_coleta_solicitante FOREIGN KEY (usuario_solicitante_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_coleta_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id)
);

CREATE INDEX idx_coletas_solicitante ON tb_coletas(usuario_solicitante_id);
CREATE INDEX idx_coletas_status ON tb_coletas(status);
CREATE INDEX idx_coletas_data ON tb_coletas(data_hora_agendamento);