package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.enums.TipoMaterial;

public record MaterialResposta(
        String codigo,
        String nome,
        String descricao,
        String unidadePadrao,
        double fatorCarbonoEvitadoKg
) {
    public static MaterialResposta converter(TipoMaterial material) {
        return new MaterialResposta(
                material.name(),
                material.getNomeExibicao(),
                material.getDescricao(),
                material.getUnidadePadrao(),
                material.getFatorCarbonoEvitadoKg()
        );
    }
}