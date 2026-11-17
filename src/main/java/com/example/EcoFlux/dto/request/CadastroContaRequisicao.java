package com.example.EcoFlux.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroContaRequisicao(
        @NotNull @Valid UsuarioCadastroRequisicao usuario,
        @NotBlank @Size(max = 72) String confirmarSenha,
        @Size(max = 14) String cpf,
        @Valid InstituicaoCadastroRequisicao instituicao
) {}
