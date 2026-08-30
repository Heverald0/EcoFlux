CREATE TABLE tb_instituicoes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    documento VARCHAR(20) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    cep VARCHAR(10) NOT NULL,
    usuario_responsavel_id BIGINT NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_instituicao_usuario FOREIGN KEY (usuario_responsavel_id) REFERENCES tb_usuarios(id)
);

CREATE INDEX idx_instituicoes_documento ON tb_instituicoes(documento);
CREATE INDEX idx_instituicoes_responsavel ON tb_instituicoes(usuario_responsavel_id);