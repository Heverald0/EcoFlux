package com.example.EcoFlux.entity.enums;

import java.util.Set;

public enum StatusColeta {
    ENVIADA,
    EM_ANALISE,
    AGENDADA,
    EM_ROTA,
    EM_ANDAMENTO,
    REALIZADA,
    CONCLUIDA,
    CANCELADA;

    public boolean podeTransicionarPara(StatusColeta novoStatus) {
        if (this == novoStatus) {
            return false;
        }
        return switch (this) {
            case ENVIADA -> Set.of(EM_ANALISE, AGENDADA, CANCELADA).contains(novoStatus);
            case EM_ANALISE -> Set.of(AGENDADA, CANCELADA).contains(novoStatus);
            case AGENDADA -> Set.of(EM_ROTA, EM_ANDAMENTO, CANCELADA).contains(novoStatus);
            case EM_ROTA, EM_ANDAMENTO -> Set.of(REALIZADA, CONCLUIDA, CANCELADA).contains(novoStatus);
            case REALIZADA, CONCLUIDA, CANCELADA -> false;
        };
    }
}