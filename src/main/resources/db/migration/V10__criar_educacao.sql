CREATE TABLE tb_conteudos_educativos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    url_midia VARCHAR(1500),
    publicado BOOLEAN NOT NULL,
    ordem INT NOT NULL,
    destaque BOOLEAN NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE tb_perguntas_quiz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conteudo_id BIGINT NOT NULL,
    enunciado VARCHAR(1000) NOT NULL,
    ordem INT NOT NULL,
    alternativa_correta INT NOT NULL,
    explicacao VARCHAR(2000) NOT NULL,
    CONSTRAINT fk_pergunta_conteudo FOREIGN KEY (conteudo_id) REFERENCES tb_conteudos_educativos(id)
);

CREATE TABLE tb_alternativas_quiz (
    pergunta_id BIGINT NOT NULL,
    ordem INT NOT NULL,
    texto VARCHAR(1000) NOT NULL,
    PRIMARY KEY (pergunta_id, ordem),
    CONSTRAINT fk_alternativa_pergunta FOREIGN KEY (pergunta_id) REFERENCES tb_perguntas_quiz(id)
);

CREATE TABLE tb_progresso_conteudo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    conteudo_id BIGINT NOT NULL,
    percentual INT NOT NULL,
    iniciado_em TIMESTAMP NOT NULL,
    concluido_em TIMESTAMP NULL,
    CONSTRAINT uk_progresso_usuario_conteudo UNIQUE (usuario_id, conteudo_id),
    CONSTRAINT fk_progresso_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_progresso_conteudo FOREIGN KEY (conteudo_id) REFERENCES tb_conteudos_educativos(id)
);

CREATE TABLE tb_tentativas_quiz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    conteudo_id BIGINT NOT NULL,
    acertos INT NOT NULL,
    total INT NOT NULL,
    aprovado BOOLEAN NOT NULL,
    respondido_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_tentativa_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_tentativa_conteudo FOREIGN KEY (conteudo_id) REFERENCES tb_conteudos_educativos(id)
);

CREATE TABLE tb_campanhas_educativas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    inicio DATE NOT NULL,
    fim DATE NOT NULL,
    url_acao VARCHAR(1500),
    publicada BOOLEAN NOT NULL
);

CREATE TABLE tb_arquivos_educativos (
    conteudo_id BIGINT PRIMARY KEY,
    dados LONGBLOB NOT NULL,
    CONSTRAINT fk_arquivo_conteudo FOREIGN KEY (conteudo_id) REFERENCES tb_conteudos_educativos(id)
);

CREATE INDEX idx_conteudos_publicacao ON tb_conteudos_educativos(publicado, ordem);
CREATE INDEX idx_tentativas_usuario_conteudo ON tb_tentativas_quiz(usuario_id, conteudo_id);
CREATE INDEX idx_campanhas_publicacao_periodo ON tb_campanhas_educativas(publicada, inicio, fim);
