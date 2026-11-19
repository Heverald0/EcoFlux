package com.example.EcoFlux.dto;

import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AmbientalDtos {
    private AmbientalDtos() {}

    public record Pontos(long frequencia, long separacao, long quantidade, long regularidade, long total) {}

    public record Score(Long instituicaoId, Pontos pontos, NivelSelo nivel, long proximoNivel,
                        String regraVersao, LocalDate inicio, LocalDate fim) {}

    public record Movimento(Long id, Long materialId, Pontos pontos, String regraVersao, LocalDate data) {
        public static Movimento de(MovimentoScore m) {
            return new Movimento(m.getId(), m.getMaterial().getId(), new Pontos(m.getFrequencia(),
                    m.getSeparacao(), m.getQuantidade(), m.getRegularidade(), m.getTotal()), m.getRegraVersao(), m.getDataReferencia());
        }
    }

    public record Ranking(Long instituicaoId, String nome, long pontos, NivelSelo nivel) {}
}
