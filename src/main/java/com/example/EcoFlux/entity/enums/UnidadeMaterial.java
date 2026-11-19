package com.example.EcoFlux.entity.enums;

public enum UnidadeMaterial {
    KG, L;

    public boolean aceita(TipoMaterial material) {
        return material == TipoMaterial.OLEO || this == KG;
    }
}
