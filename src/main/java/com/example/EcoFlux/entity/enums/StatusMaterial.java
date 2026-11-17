package com.example.EcoFlux.entity.enums;

public enum StatusMaterial {
    AGENDADA, TRIAGEM, COLETADO, COMERCIALIZADO;

    public boolean permite(StatusMaterial proximo) {
        return switch (this) {
            case AGENDADA -> proximo == TRIAGEM;
            case TRIAGEM -> proximo == COLETADO;
            case COLETADO -> proximo == COMERCIALIZADO;
            case COMERCIALIZADO -> false;
        };
    }

    public boolean contabilizado() {
        return this == COLETADO || this == COMERCIALIZADO;
    }
}
