package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.TipoInstituicao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InstituicaoCadastroRequisicao(
        @NotBlank(message = "O nome da instituicao e obrigatorio.")
        @Size(min = 3, max = 150, message = "O nome deve conter entre 3 e 150 caracteres.")
        String nome,

        @NotBlank(message = "O documento (CNPJ/identificador) e obrigatorio.")
        @Size(min = 11, max = 20, message = "O documento deve conter entre 11 e 20 caracteres.")
        String documento,

        @NotNull(message = "O tipo da instituicao e obrigatorio (EMPRESA, CONDOMINIO ou COOPERATIVA).")
        TipoInstituicao tipo,

        @NotBlank(message = "O telefone de contato e obrigatorio.")
        String telefone,

        @NotBlank(message = "O logradouro e obrigatorio.")
        String logradouro,

        @NotBlank(message = "O numero e obrigatorio.")
        String numero,

        @NotBlank(message = "O bairro e obrigatorio.")
        String bairro,

        @NotBlank(message = "A cidade e obrigatoria.")
        String cidade,

        @NotBlank(message = "O estado (UF) e obrigatorio.")
        @Size(min = 2, max = 2, message = "O estado deve conter exatamente 2 caracteres (ex: BA, SP).")
        String estado,

        @NotBlank(message = "O CEP e obrigatorio.")
        String cep
) {}