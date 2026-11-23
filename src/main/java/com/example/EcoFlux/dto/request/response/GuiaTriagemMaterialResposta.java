package com.example.EcoFlux.dto.request.response;

import java.util.List;

public record GuiaTriagemMaterialResposta(
        String codigoMaterial,
        String nomeMaterial,
        String corColetaSeletiva,
        double fatorCo2EvitadoPorKg,
        int pontosEcoScorePorKg,
        List<String> podeReciclar,
        List<String> naoPodeReciclar,
        String instrucaoPreparo
) {
}