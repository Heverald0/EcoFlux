package com.example.EcoFlux.dto.request.response;

public record TokenResposta(
        String token,
        String tipo,
        Long expiraEmSegundos
) {
    public TokenResposta(String token, Long expiraEmSegundos) {
        this(token, "Bearer", expiraEmSegundos);
    }
}