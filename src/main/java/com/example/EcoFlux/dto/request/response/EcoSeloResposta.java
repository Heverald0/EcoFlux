package com.example.EcoFlux.dto.request.response;

public record EcoSeloResposta(
        String codigo,
        String titulo,
        String descricao,
        String criterio,
        String icone,
        boolean conquistado,
        int progressoAtual,
        int metaExigida,
        int progressoPercentual
) {}