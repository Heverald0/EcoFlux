ALTER TABLE tb_coletas ADD COLUMN unidade VARCHAR(10) NOT NULL DEFAULT 'KG';
ALTER TABLE tb_coletas ADD COLUMN quantidade_real DECIMAL(12,2);
ALTER TABLE tb_coletas ADD COLUMN data_conclusao TIMESTAMP;
ALTER TABLE tb_coletas ADD COLUMN ponto_coleta_id BIGINT;
ALTER TABLE tb_coletas ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tb_coletas ADD CONSTRAINT fk_coleta_ponto FOREIGN KEY (ponto_coleta_id) REFERENCES tb_pontos_coleta(id);
CREATE INDEX idx_coletas_instituicao_data ON tb_coletas(instituicao_id, data_hora_agendamento);
CREATE INDEX idx_coletas_cooperativa_status ON tb_coletas(cooperativa_id, status);
CREATE TABLE tb_eventos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chave VARCHAR(150) NOT NULL UNIQUE,
    tipo VARCHAR(80) NOT NULL,
    entidade VARCHAR(80) NOT NULL,
    entidade_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    destinatario_id BIGINT,
    instituicao_id BIGINT,
    mensagem VARCHAR(1000) NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_leitura TIMESTAMP,
    CONSTRAINT fk_evento_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_evento_destinatario FOREIGN KEY (destinatario_id) REFERENCES tb_usuarios(id),
    CONSTRAINT fk_evento_instituicao FOREIGN KEY (instituicao_id) REFERENCES tb_instituicoes(id)
);
CREATE INDEX idx_eventos_destinatario_data ON tb_eventos(destinatario_id, data_criacao);
