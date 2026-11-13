package com.example.EcoFlux.dto.request;

import com.example.EcoFlux.entity.enums.TipoMaterial;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record PontoColetaRequisicao(
        @NotBlank(message = "O nome do ponto de coleta e obrigatorio.")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres.")
        String nome,

        @NotBlank(message = "O nome do responsavel pelo ponto e obrigatorio.")
        @Size(max = 120, message = "O nome do responsavel deve ter no maximo 120 caracteres.")
        String nomeResponsavel,

        @Size(max = 20, message = "O telefone de contato deve ter no maximo 20 caracteres.")
        String telefoneContato,

        @NotBlank(message = "O horario de funcionamento e obrigatorio.")
        @Size(max = 120, message = "O horario de funcionamento deve ter no maximo 120 caracteres.")
        String horarioFuncionamento,

        @NotBlank(message = "O logradouro e obrigatorio.")
        String logradouro,

        @NotBlank(message = "O numero e obrigatorio.")
        String numero,

        @NotBlank(message = "O bairro e obrigatorio.")
        String bairro,

        @NotBlank(message = "A cidade e obrigatoria.")
        String cidade,

        @NotBlank(message = "O estado (UF) e obrigatorio.")
        @Size(min = 2, max = 2, message = "O estado deve conter 2 caracteres.")
        String estado,

        @NotBlank(message = "O CEP e obrigatorio.")
        String cep,

        @NotNull(message = "A latitude e obrigatoria para posicionamento no mapa.")
        @DecimalMin(value = "-90.0", message = "Latitude minima permitida e -90.0.")
        @DecimalMax(value = "90.0", message = "Latitude maxima permitida e 90.0.")
        BigDecimal latitude,

        @NotNull(message = "A longitude e obrigatoria para posicionamento no mapa.")
        @DecimalMin(value = "-180.0", message = "Longitude minima permitida e -180.0.")
        @DecimalMax(value = "180.0", message = "Longitude maxima permitida e 180.0.")
        BigDecimal longitude,

        @NotEmpty(message = "Informe ao menos um tipo de material reciclavel aceito no ponto.")
        Set<TipoMaterial> materiaisAceitos,

        Long instituicaoId
) {}