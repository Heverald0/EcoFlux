package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.MembroInstituicao;
import com.example.EcoFlux.entity.enums.PapelMembroInstituicao;

import java.time.LocalDateTime;

public record MembroInstituicaoResposta(
        Long id,
        Long instituicaoId,
        String nomeInstituicao,
        Long usuarioId,
        String nomeUsuario,
        String emailUsuario,
        String cpfUsuario,
        PapelMembroInstituicao papel,
        Boolean ativo,
        LocalDateTime dataVinculo
) {
    public static MembroInstituicaoResposta converter(MembroInstituicao entity) {
        return new MembroInstituicaoResposta(
                entity.getId(),
                entity.getInstituicao().getId(),
                entity.getInstituicao().getNome(),
                entity.getUsuario().getId(),
                entity.getUsuario().getNome(),
                entity.getUsuario().getEmail(),
                entity.getUsuario().getDocumento(),
                entity.getPapel(),
                entity.getAtivo(),
                entity.getDataVinculo()
        );
    }
}