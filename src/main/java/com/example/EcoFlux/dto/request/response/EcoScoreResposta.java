package com.example.EcoFlux.dto.request.response;

import java.math.BigDecimal;

public record EcoScoreResposta(
        Long usuarioId,
        String nomeUsuario,
        int pontuacaoTotal,
        String nivel,
        int proximoNivelPontos,
        int progressoPercentualNivel,
        long coletasConcluidas,
        int diversidadeMateriais,
        BigDecimal totalRecicladoKg
) {}