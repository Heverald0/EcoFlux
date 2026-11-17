package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.enums.StatusColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import com.example.EcoFlux.entity.enums.UnidadeMaterial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ColetaResposta(
        Long id,
        LocalDateTime dataHoraAgendamento,
        String janelaHorario,
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
        Long idCooperativa,
        String nomeCooperativa,
        LocalDateTime dataCriacao,
        Long pontoColetaId,
        UnidadeMaterial unidade,
        BigDecimal quantidadeReal,
        LocalDateTime dataConclusao
) {
    public static ColetaResposta converter(Coleta coleta) {
        return new ColetaResposta(
                coleta.getId(),
                coleta.getDataHoraAgendamento(),
                coleta.getJanelaHorario(),
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
                coleta.getCooperativa() != null ? coleta.getCooperativa().getId() : null,
                coleta.getCooperativa() != null ? coleta.getCooperativa().getNome() : null,
                coleta.getDataCriacao(),
                coleta.getPontoColeta() != null ? coleta.getPontoColeta().getId() : null,
                coleta.getUnidade(),
                coleta.getQuantidadeReal(),
                coleta.getDataConclusao()
        );
    }
}
