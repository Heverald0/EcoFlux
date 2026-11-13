package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.StatusColeta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarStatusColetaRequisicao(
        @NotNull(message = "O novo status da coleta e obrigatorio.")
        StatusColeta novoStatus,

        @Size(max = 255, message = "A observacao deve ter no maximo 255 caracteres.")
        String observacao,

        Long cooperativaId
) {}