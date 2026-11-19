package com.example.EcoFlux.service;

import com.example.EcoFlux.entity.enums.NivelSelo;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Getter
public class RegraEcoScore {
    @Value("${ecoflux.score.versao:1}") private String versao;
    @Value("${ecoflux.score.frequencia:10}") private int frequencia;
    @Value("${ecoflux.score.separacao:5}") private int separacao;
    @Value("${ecoflux.score.pontos-por-unidade:0.1}") private BigDecimal pontosPorUnidade;
    @Value("${ecoflux.score.limite-quantidade:100}") private int limiteQuantidade;
    @Value("${ecoflux.score.regularidade:10}") private int regularidade;
    @Value("${ecoflux.score.janela-regularidade-dias:30}") private int janelaRegularidadeDias;
    @Value("${ecoflux.selo.bronze:100}") private long bronze;
    @Value("${ecoflux.selo.prata:500}") private long prata;
    @Value("${ecoflux.selo.ouro:1000}") private long ouro;
    @Value("${ecoflux.selo.validade-dias:365}") private int validadeDias;

    @PostConstruct
    void validar() {
        if (versao.isBlank() || frequencia < 0 || separacao < 0 || pontosPorUnidade.signum() < 0
                || limiteQuantidade < 0 || regularidade < 0 || janelaRegularidadeDias < 1
                || bronze < 1 || prata <= bronze || ouro <= prata || validadeDias < 1) {
            throw new IllegalStateException("Configuracao invalida de EcoScore/EcoSelo.");
        }
    }

    public int quantidade(BigDecimal quantidade) {
        return quantidade.multiply(pontosPorUnidade).min(BigDecimal.valueOf(limiteQuantidade))
                .setScale(0, RoundingMode.DOWN).intValueExact();
    }

    public int separacao(TipoMaterial tipo) {
        return tipo == TipoMaterial.MISTO ? 0 : separacao;
    }

    public NivelSelo nivel(long total) {
        if (total >= ouro) return NivelSelo.OURO;
        if (total >= prata) return NivelSelo.PRATA;
        if (total >= bronze) return NivelSelo.BRONZE;
        return NivelSelo.INICIAL;
    }

    public long proximo(long total) {
        if (total < bronze) return bronze - total;
        if (total < prata) return prata - total;
        if (total < ouro) return ouro - total;
        return 0;
    }
}
