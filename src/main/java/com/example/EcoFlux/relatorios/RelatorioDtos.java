package com.example.EcoFlux.relatorios;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class RelatorioDtos {
    private RelatorioDtos() {}
    public record Gerar(@NotBlank @Size(max = 150) String nome, @NotNull TipoRelatorio tipo,
                        @NotNull LocalDate inicio, @NotNull LocalDate fim, Long instituicaoId) {}
    public record Resposta(Long id, String nome, TipoRelatorio tipo, LocalDate inicio, LocalDate fim,
                          Long instituicaoId, String formato, String status, LocalDateTime geradoEm, long exportacoes) {
        public static Resposta de(Relatorio r) {
            return new Resposta(r.getId(), r.getNome(), r.getTipo(), r.getInicio(), r.getFim(), r.getInstituicaoId(),
                    "PDF", r.getStatus(), r.getGeradoEm(), r.getExportacoes());
        }
    }
    public record Detalhe(Resposta relatorio, JsonNode dados) {}
}
