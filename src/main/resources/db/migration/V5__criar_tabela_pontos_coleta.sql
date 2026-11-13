CREATE TABLE tb_pontos_coleta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    nome_responsavel VARCHAR(120) NOT NULL,
    telefone_contato VARCHAR(20),
    horario_funcionamento VARCHAR(120) NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    cep VARCHAR(10) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    motivo_status VARCHAR(255),
    usuario_cadastro_id BIGINT NOT NULL,
    instituicao_id BIGINT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ponto_usuario_cadastro FOREIGN KEY (usuario_cadastro_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_ponto_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id)
);

CREATE TABLE tb_ponto_coleta_materiais (
    ponto_coleta_id BIGINT NOT NULL,
    tipo_material VARCHAR(50) NOT NULL,
    PRIMARY KEY (ponto_coleta_id, tipo_material),
    CONSTRAINT fk_ponto_materiais FOREIGN KEY (ponto_coleta_id) REFERENCES tb_pontos_coleta(id) ON DELETE CASCADE
);

CREATE INDEX idx_pontos_coleta_status ON tb_pontos_coleta(status);
CREATE INDEX idx_pontos_coleta_bairro ON tb_pontos_coleta(bairro);
CREATE INDEX idx_pontos_coleta_cidade ON tb_pontos_coleta(cidade);