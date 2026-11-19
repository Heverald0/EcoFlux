package com.example.EcoFlux.rotas;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class RotaRequisicoes {
    private RotaRequisicoes() {}

    public record Planejar(
            @NotBlank @Size(max = 150) String nome,
            @NotNull @Positive Long cooperativaId,
            @NotNull @Future LocalDateTime partida,
            @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitudeOrigem,
            @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitudeOrigem,
            @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> coletaIds) {}

    public record Concluir(@NotNull @DecimalMin(value = "0", inclusive = false)
                           @Digits(integer = 8, fraction = 2) BigDecimal quantidadeReal,
                           @Size(max = 255) String observacao) {}

    public record Falhar(@NotBlank @Size(max = 255) String motivo) {}
}
