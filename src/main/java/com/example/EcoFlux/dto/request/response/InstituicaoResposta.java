package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.entity.enums.StatusInstituicao;

import java.time.LocalDateTime;

public record InstituicaoResposta(
        Long id,
        String nome,
        String documento,
        TipoInstituicao tipo,
        String telefone,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        Long idResponsavel,
        String nomeResponsavel,
        LocalDateTime dataCriacao,
        StatusInstituicao status,
        String motivoStatus,
        LocalDateTime dataAnalise
) {
    public static InstituicaoResposta converter(Instituicao instituicao) {
        return new InstituicaoResposta(
                instituicao.getId(),
                instituicao.getNome(),
                instituicao.getDocumento(),
                instituicao.getTipo(),
                instituicao.getTelefone(),
                instituicao.getLogradouro(),
                instituicao.getNumero(),
                instituicao.getBairro(),
                instituicao.getCidade(),
                instituicao.getEstado(),
                instituicao.getCep(),
                instituicao.getUsuarioResponsavel().getId(),
                instituicao.getUsuarioResponsavel().getNome(),
                instituicao.getDataCriacao(),
                instituicao.getStatus(),
                instituicao.getMotivoStatus(),
                instituicao.getDataAnalise()
        );
    }
}
