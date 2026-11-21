package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroRequisicao(
        @NotBlank(message = "O nome e obrigatorio.")
        @Size(min = 3, max = 150, message = "O nome deve conter entre 3 e 150 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "Formato de e-mail invalido.")
        @Size(max = 150, message = "O e-mail nao pode ultrapassar 150 caracteres.")
        String email,

        @NotBlank(message = "A senha e obrigatoria.")
        @Size(min = 8, message = "A senha deve conter no minimo 8 caracteres.")
        String senha,

        @JsonAlias({"cpf", "cnpj", "cpfCnpj"})
        @Size(max = 20, message = "O documento deve ter no maximo 20 caracteres.")
        String documento,

        @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres.")
        String telefone,

        @JsonAlias({"tipoUsuario", "tipo"})
        PerfilUsuario perfil
) {
    public UsuarioCadastroRequisicao(String nome, String email, String senha) {
        this(nome, email, senha, null, null, null);
    }
}