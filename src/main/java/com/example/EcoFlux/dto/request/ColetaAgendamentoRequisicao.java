package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.TipoMaterial;
import com.example.EcoFlux.entity.enums.UnidadeMaterial;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ColetaAgendamentoRequisicao(
        @NotNull(message = "A data e o horario do agendamento sao obrigatorios.")
        @Future(message = "A data e horario do agendamento devem estar no futuro.")
        LocalDateTime dataHoraAgendamento,

        @Size(max = 50, message = "A janela de horario deve ter no maximo 50 caracteres.")
        String janelaHorario,

        @NotNull(message = "O tipo do material e obrigatorio.")
        TipoMaterial tipoMaterial,

        @NotNull(message = "A quantidade estimada em KG e obrigatoria.")
        @DecimalMin(value = "0.1", message = "A quantidade estimada deve ser no minimo 0.1 kg.")
        @Digits(integer = 8, fraction = 2)
        BigDecimal quantidadeEstimadaKg,

        @Size(max = 255, message = "As observacoes devem ter no maximo 255 caracteres.")
        String observacoes,

        @NotBlank(message = "O logradouro e obrigatorio.")
        @Size(max = 150)
        String logradouro,

        @NotBlank(message = "O numero e obrigatorio.")
        @Size(max = 20)
        String numero,

        @NotBlank(message = "O bairro e obrigatorio.")
        @Size(max = 100)
        String bairro,

        @NotBlank(message = "A cidade e obrigatoria.")
        @Size(max = 100)
        String cidade,

        @NotBlank(message = "O estado (UF) e obrigatorio.")
        @Size(min = 2, max = 2, message = "O estado deve conter 2 caracteres.")
        String estado,

        @NotBlank(message = "O CEP e obrigatorio.")
        @Pattern(regexp = "\\d{5}-?\\d{3}")
        String cep,

        Long instituicaoId,

        Long cooperativaId,
        Long pontoColetaId,
        UnidadeMaterial unidade
) {}
