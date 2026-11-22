package com.example.EcoFlux.dto.request.response;

import java.math.BigDecimal;

public record ImpactoPorMaterialResposta(
        String codigoMaterial,
        String nomeMaterial,
        long quantidadeColetas,
        BigDecimal pesoTotalKg,
        BigDecimal co2EvitadoKg
) {}