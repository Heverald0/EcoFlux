package com.example.EcoFlux.rotas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record RotaResposta(Long id, String nome, Long cooperativaId, StatusRota status,
                           LocalDateTime partida, BigDecimal latitudeOrigem, BigDecimal longitudeOrigem,
                           BigDecimal distanciaKm, int duracaoMinutos, String metodoEstimativa,
                           int progressoPercentual, LocalDateTime iniciadaEm, LocalDateTime concluidaEm,
                           List<Parada> paradas) {
    public record Parada(Long id, int ordem, Long coletaId, Long pontoId, String ponto,
                         String material, BigDecimal quantidadeEstimada, String unidade,
                         BigDecimal latitude, BigDecimal longitude, StatusParada status,
                         BigDecimal distanciaTrechoKm, LocalDateTime previsao,
                         LocalDateTime concluidaEm, String motivo) {}

    public static RotaResposta de(Rota rota) {
        long finalizadas = rota.getParadas().stream().filter(p -> p.getStatus() == StatusParada.CONCLUIDA
                || p.getStatus() == StatusParada.NAO_ATENDIDA).count();
        var paradas = rota.getParadas().stream().sorted(Comparator.comparingInt(ParadaRota::getOrdem))
                .map(p -> new Parada(p.getId(), p.getOrdem(), p.getColeta().getId(), p.getPonto().getId(),
                        p.getPonto().getNome(), p.getColeta().getTipoMaterial().name(),
                        p.getColeta().getQuantidadeEstimadaKg(), p.getColeta().getUnidade().name(),
                        p.getLatitude(), p.getLongitude(), p.getStatus(), p.getDistanciaTrechoKm(),
                        p.getPrevisao(), p.getConcluidaEm(), p.getMotivo())).toList();
        return new RotaResposta(rota.getId(), rota.getNome(), rota.getCooperativa().getId(), rota.getStatus(),
                rota.getPartida(), rota.getLatitudeOrigem(), rota.getLongitudeOrigem(), rota.getDistanciaKm(),
                rota.getDuracaoMinutos(), "HAVERSINE_VIZINHO_MAIS_PROXIMO_ESTIMATIVA_SEM_MALHA_VIARIA",
                paradas.isEmpty() ? 0 : (int) (finalizadas * 100 / paradas.size()),
                rota.getIniciadaEm(), rota.getConcluidaEm(), paradas);
    }
}
