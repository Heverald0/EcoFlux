CREATE TABLE tb_relatorios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    inicio DATE NOT NULL,
    fim DATE NOT NULL,
    autor_id BIGINT NOT NULL,
    instituicao_id BIGINT,
    status VARCHAR(30) NOT NULL,
    dados LONGTEXT NOT NULL,
    gerado_em TIMESTAMP NOT NULL,
    exportacoes BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_relatorio_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_relatorio_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id)
);
CREATE INDEX idx_relatorios_autor_data ON tb_relatorios(autor_id, gerado_em);
