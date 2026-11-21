package com.example.EcoFlux.dto.request.response;

import java.math.BigDecimal;
import java.util.List;

public record EcoImpactoResposta(
        long totalColetasConcluidas,
        BigDecimal totalResiduosRecicladosKg,
        BigDecimal co2EvitadoKg,
        BigDecimal arvoresPreservadasEquivalentes,
        BigDecimal energiaEconomizadaKwh,
        BigDecimal aguaEconomizadaLitros,
        List<ImpactoPorMaterialResposta> detalhamentoPorMaterial
) {}    