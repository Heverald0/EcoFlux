package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarUsuarioRequisicao(StatusUsuario status, PerfilUsuario perfil,
        @NotBlank @Size(max = 500) String motivo) {}
