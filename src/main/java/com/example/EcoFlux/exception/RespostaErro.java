package com.example.EcoFlux.exception;

import java.time.Instant;
import java.util.Map;

public record RespostaErro(
        Instant momento,
        int status,
        String erro,
        String mensagem,
        String caminho,
        Map<String, String> errosCampos
) {
    public RespostaErro(int status, String erro, String mensagem, String caminho) {
        this(Instant.now(), status, erro, mensagem, caminho, null);
    }

    public RespostaErro(int status, String erro, String mensagem, String caminho, Map<String, String> errosCampos) {
        this(Instant.now(), status, erro, mensagem, caminho, errosCampos);
    }
}