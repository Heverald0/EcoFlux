package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.StatusCadastro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarStatusCadastroRequisicao(
        @NotNull(message = "O novo status e obrigatorio.")
        StatusCadastro status,

        @Size(max = 255, message = "O motivo deve ter no maximo 255 caracteres.")
        String motivo
) {}