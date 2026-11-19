CREATE TABLE tb_fatores_impacto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material VARCHAR(30) NOT NULL,
    unidade VARCHAR(30) NOT NULL,
    versao VARCHAR(50) NOT NULL,
    fonte VARCHAR(1000) NOT NULL,
    vigencia DATE NOT NULL,
    co2_kg DECIMAL(18,6) NOT NULL,
    agua_l DECIMAL(18,6) NOT NULL,
    energia_kwh DECIMAL(18,6) NOT NULL,
    arvores DECIMAL(18,6) NOT NULL,
    autor_id BIGINT NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_fator_versao UNIQUE (material, unidade, versao),
    CONSTRAINT fk_fator_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id)
);

CREATE INDEX idx_fator_vigencia ON tb_fatores_impacto(material, unidade, vigencia);

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
    fator_impacto_id BIGINT,
    versao BIGINT NOT NULL DEFAULT 0,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_material_quantidade CHECK (quantidade > 0),
    CONSTRAINT fk_material_coleta FOREIGN KEY (coleta_id) REFERENCES tb_coletas(id),
    CONSTRAINT fk_material_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id),
    CONSTRAINT fk_material_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_material_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_material_fator FOREIGN KEY (fator_impacto_id) REFERENCES tb_fatores_impacto(id)
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

CREATE TABLE tb_certificados_ambientais (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instituicao_id BIGINT NOT NULL,
    nivel VARCHAR(30) NOT NULL,
    score_base BIGINT NOT NULL,
    regra_versao VARCHAR(50) NOT NULL,
    codigo VARCHAR(36) NOT NULL UNIQUE,
    emissao DATE NOT NULL,
    validade DATE NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    substituido BOOLEAN NOT NULL DEFAULT FALSE,
    autor_id BIGINT NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_selo_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id),
    CONSTRAINT fk_selo_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id)
);

CREATE INDEX idx_selo_instituicao_emissao ON tb_certificados_ambientais(instituicao_id, emissao);

CREATE TABLE tb_metas_ambientais (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instituicao_id BIGINT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    indicador VARCHAR(30) NOT NULL,
    alvo DECIMAL(18,6) NOT NULL,
    inicio DATE NOT NULL,
    fim DATE NOT NULL,
    autor_id BIGINT NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_meta_alvo CHECK (alvo > 0),
    CONSTRAINT fk_meta_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id),
    CONSTRAINT fk_meta_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id)
);

CREATE INDEX idx_meta_instituicao ON tb_metas_ambientais(instituicao_id, fim);
