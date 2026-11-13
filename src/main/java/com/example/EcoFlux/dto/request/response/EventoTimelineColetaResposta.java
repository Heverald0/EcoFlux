package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.HistoricoStatusColeta;
import com.example.EcoFlux.entity.enums.StatusColeta;

import java.time.LocalDateTime;

public record EventoTimelineColetaResposta(
        Long idEvento,
        StatusColeta statusAnterior,
        StatusColeta statusNovo,
        String observacao,
        Long idAutor,
        String nomeAutor,
        LocalDateTime dataRegistro
) {
    public static EventoTimelineColetaResposta converter(HistoricoStatusColeta historico) {
        return new EventoTimelineColetaResposta(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getObservacao(),
                historico.getUsuarioAutor().getId(),
                historico.getUsuarioAutor().getNome(),
                historico.getDataRegistro()
        );
    }
}