package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.PapelMembroInstituicao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VincularMembroRequisicao(
        @NotBlank(message = "O CPF do usuario e obrigatorio para validacao do vinculo.")
        String cpf,

        @NotBlank(message = "O e-mail do usuario e obrigatorio para validacao do vinculo.")
        @Email(message = "Informe um e-mail valido.")
        String email,

        @NotNull(message = "Informe o papel do membro (MORADOR ou MOTORISTA).")
        PapelMembroInstituicao papel
) {
}