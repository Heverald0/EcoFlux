package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.PontoColeta;
import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record PontoColetaResposta(
        Long id,
        String nome,
        String nomeResponsavel,
        String telefoneContato,
        String horarioFuncionamento,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        BigDecimal latitude,
        BigDecimal longitude,
        StatusPontoColeta status,
        String motivoStatus,
        Set<TipoMaterial> materiaisAceitos,
        Long idInstituicao,
        String nomeInstituicao,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
) {
    public static PontoColetaResposta converter(PontoColeta ponto) {
        return new PontoColetaResposta(
                ponto.getId(),
                ponto.getNome(),
                ponto.getNomeResponsavel(),
                ponto.getTelefoneContato(),
                ponto.getHorarioFuncionamento(),
                ponto.getLogradouro(),
                ponto.getNumero(),
                ponto.getBairro(),
                ponto.getCidade(),
                ponto.getEstado(),
                ponto.getCep(),
                ponto.getLatitude(),
                ponto.getLongitude(),
                ponto.getStatus(),
                ponto.getMotivoStatus(),
                ponto.getMateriaisAceitos(),
                ponto.getInstituicao() != null ? ponto.getInstituicao().getId() : null,
                ponto.getInstituicao() != null ? ponto.getInstituicao().getNome() : null,
                ponto.getDataCriacao(),
                ponto.getDataAtualizacao()
        );
    }
}