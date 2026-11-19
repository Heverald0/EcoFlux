package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.StatusColeta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record AtualizarStatusColetaRequisicao(
        @NotNull(message = "O novo status da coleta e obrigatorio.")
        StatusColeta novoStatus,

        @Size(max = 255, message = "A observacao deve ter no maximo 255 caracteres.")
        String observacao,

        Long cooperativaId,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
        BigDecimal quantidadeReal
) {
    public AtualizarStatusColetaRequisicao(StatusColeta novoStatus, String observacao, Long cooperativaId) {
        this(novoStatus, observacao, cooperativaId, null);
    }
}
