package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.enums.StatusColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ColetaResposta(
        Long id,
        LocalDateTime dataHoraAgendamento,
        TipoMaterial tipoMaterial,
        BigDecimal quantidadeEstimadaKg,
        String observacoes,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        StatusColeta status,
        Long idSolicitante,
        String nomeSolicitante,
        Long idInstituicao,
        String nomeInstituicao,
        LocalDateTime dataCriacao
) {
    public static ColetaResposta converter(Coleta coleta) {
        return new ColetaResposta(
                coleta.getId(),
                coleta.getDataHoraAgendamento(),
                coleta.getTipoMaterial(),
                coleta.getQuantidadeEstimadaKg(),
                coleta.getObservacoes(),
                coleta.getLogradouro(),
                coleta.getNumero(),
                coleta.getBairro(),
                coleta.getCidade(),
                coleta.getEstado(),
                coleta.getCep(),
                coleta.getStatus(),
                coleta.getUsuarioSolicitante().getId(),
                coleta.getUsuarioSolicitante().getNome(),
                coleta.getInstituicao() != null ? coleta.getInstituicao().getId() : null,
                coleta.getInstituicao() != null ? coleta.getInstituicao().getNome() : null,
                coleta.getDataCriacao()
        );
    }
}