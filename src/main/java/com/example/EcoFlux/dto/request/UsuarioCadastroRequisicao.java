package com.example.EcoFlux.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroRequisicao(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome deve conter entre 3 e 150 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        @Size(max = 150, message = "O e-mail não pode ultrapassar 150 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, message = "A senha deve conter no mínimo 8 caracteres.")
        String senha
) {}