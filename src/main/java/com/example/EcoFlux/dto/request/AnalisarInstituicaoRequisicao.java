package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.StatusInstituicao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnalisarInstituicaoRequisicao(@NotNull StatusInstituicao status,
        @NotBlank @Size(max = 500) String motivo) {}
