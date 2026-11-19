CREATE TABLE tb_registros_material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo_material VARCHAR(30) NOT NULL,
    quantidade DECIMAL(14,3) NOT NULL,
    unidade VARCHAR(30) NOT NULL,
    volume DECIMAL(14,3),
    origem VARCHAR(200) NOT NULL,
    destino VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_registro DATE NOT NULL,
    data_efetivacao TIMESTAMP,
    coleta_id BIGINT UNIQUE,
    instituicao_id BIGINT,
    usuario_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_material_quantidade CHECK (quantidade > 0),
    CONSTRAINT fk_material_coleta FOREIGN KEY (coleta_id) REFERENCES tb_coletas(id),
    CONSTRAINT fk_material_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id),
    CONSTRAINT fk_material_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_material_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id)
);

CREATE INDEX idx_material_instituicao_periodo ON tb_registros_material(instituicao_id, data_registro, status);
CREATE INDEX idx_material_usuario_periodo ON tb_registros_material(usuario_id, data_registro, status);

CREATE TABLE tb_movimentos_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL UNIQUE,
    instituicao_id BIGINT,
    usuario_id BIGINT NOT NULL,
    frequencia INTEGER NOT NULL,
    separacao INTEGER NOT NULL,
    quantidade INTEGER NOT NULL,
    regularidade INTEGER NOT NULL,
    total INTEGER NOT NULL,
    regra_versao VARCHAR(50) NOT NULL,
    data_referencia DATE NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_score_material FOREIGN KEY (material_id) REFERENCES tb_registros_material(id),
    CONSTRAINT fk_score_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id),
    CONSTRAINT fk_score_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id)
);

CREATE INDEX idx_score_instituicao_periodo ON tb_movimentos_score(instituicao_id, data_referencia);
CREATE INDEX idx_score_usuario_periodo ON tb_movimentos_score(usuario_id, data_referencia);
