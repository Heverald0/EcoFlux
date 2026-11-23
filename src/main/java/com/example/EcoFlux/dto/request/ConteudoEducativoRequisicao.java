package com.example.EcoFlux.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConteudoEducativoRequisicao(
        @NotBlank(message = "O slug e obrigatorio.")
        @Size(max = 120)
        String slug,

        @NotBlank(message = "A categoria e obrigatoria.")
        @Size(max = 50)
        String categoria,

        @NotBlank(message = "O titulo e obrigatorio.")
        @Size(max = 180)
        String titulo,

        @NotBlank(message = "O resumo e obrigatorio.")
        @Size(max = 400)
        String resumo,

        @NotBlank(message = "O conteudo completo e obrigatorio.")
        String corpoTexto,

        @Min(value = 1, message = "O tempo de leitura deve ser de no minimo 1 minuto.")
        Integer tempoLeituraMinutos,

        String materialRelacionado,
        List<String> boasPraticas,
        List<String> oQueEvitar,
        String impactoDestaque
) {
}