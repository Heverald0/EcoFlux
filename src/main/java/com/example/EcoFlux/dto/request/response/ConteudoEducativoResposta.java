package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.ConteudoEducativo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record ConteudoEducativoResposta(
        Long id,
        String slug,
        String categoria,
        String titulo,
        String resumo,
        String corpoTexto,
        Integer tempoLeituraMinutos,
        String materialRelacionado,
        List<String> boasPraticas,
        List<String> oQueEvitar,
        String impactoDestaque,
        Boolean ativo,
        LocalDateTime dataCriacao
) {
    public static ConteudoEducativoResposta converter(ConteudoEducativo entity) {
        return new ConteudoEducativoResposta(
                entity.getId(),
                entity.getSlug(),
                entity.getCategoria(),
                entity.getTitulo(),
                entity.getResumo(),
                entity.getCorpoTexto(),
                entity.getTempoLeituraMinutos(),
                entity.getMaterialRelacionado(),
                quebrarLista(entity.getBoasPraticas()),
                quebrarLista(entity.getOQueEvitar()),
                entity.getImpactoDestaque(),
                entity.getAtivo(),
                entity.getDataCriacao()
        );
    }

    private static List<String> quebrarLista(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texto.split(";"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}