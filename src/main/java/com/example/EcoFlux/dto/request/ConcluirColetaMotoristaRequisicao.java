package com.example.EcoFlux.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ConcluirColetaMotoristaRequisicao(
        @Positive(message = "O peso real coletado deve ser maior que zero.")
        BigDecimal pesoRealKg,

        @NotBlank(message = "Informe o nome do responsavel que acompanhou a retirada no local.")
        String nomeRecebedor,

        @NotBlank(message = "Informe os 4 ultimos digitos do CPF do solicitante.")
        @Pattern(regexp = "\\d{4}", message = "Informe exatamente os 4 ultimos digitos numericos do CPF.")
        String documentoRecebedor,

        @NotBlank(message = "A assinatura digital do responsavel no local e obrigatoria.")
        String assinaturaDigital,

        String observacoes
) {
}