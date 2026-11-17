CREATE TABLE tb_rotas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cooperativa_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    partida TIMESTAMP NOT NULL,
    latitude_origem DECIMAL(10,7) NOT NULL,
    longitude_origem DECIMAL(10,7) NOT NULL,
    status VARCHAR(30) NOT NULL,
    distancia_km DECIMAL(12,3) NOT NULL,
    duracao_minutos INT NOT NULL,
    iniciada_em TIMESTAMP NULL,
    concluida_em TIMESTAMP NULL,
    criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rota_cooperativa FOREIGN KEY (cooperativa_id) REFERENCES tb_instituicoes(id),
    CONSTRAINT fk_rota_autor FOREIGN KEY (autor_id) REFERENCES tb_usuarios(id)
);

CREATE TABLE tb_paradas_rota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rota_id BIGINT NOT NULL,
    coleta_id BIGINT NOT NULL UNIQUE,
    ponto_id BIGINT NOT NULL,
    ordem INT NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    status VARCHAR(30) NOT NULL,
    distancia_trecho_km DECIMAL(12,3) NOT NULL,
    previsao TIMESTAMP NOT NULL,
    concluida_em TIMESTAMP NULL,
    motivo VARCHAR(255),
    CONSTRAINT fk_parada_rota FOREIGN KEY (rota_id) REFERENCES tb_rotas(id),
    CONSTRAINT fk_parada_coleta FOREIGN KEY (coleta_id) REFERENCES tb_coletas(id),
    CONSTRAINT fk_parada_ponto FOREIGN KEY (ponto_id) REFERENCES tb_pontos_coleta(id)
);

CREATE INDEX idx_rotas_cooperativa_partida ON tb_rotas(cooperativa_id, partida);
CREATE INDEX idx_paradas_rota_ordem ON tb_paradas_rota(rota_id, ordem);
