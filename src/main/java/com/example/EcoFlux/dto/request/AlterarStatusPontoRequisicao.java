package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlterarStatusPontoRequisicao(
        @NotNull(message = "O novo status do ponto de coleta e obrigatorio.")
        StatusPontoColeta status,

        @Size(max = 255, message = "O motivo deve ter no maximo 255 caracteres.")
        String motivo
) {}