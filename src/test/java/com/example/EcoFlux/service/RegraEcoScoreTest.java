package com.example.EcoFlux.service;

import com.example.EcoFlux.entity.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class RegraEcoScoreTest {
    @Test
    void quantidadeTemLimiteEArredondamentoParaBaixo() {
        RegraEcoScore regra = regra();
        assertThat(regra.quantidade(new BigDecimal("19.99"))).isEqualTo(1);
        assertThat(regra.quantidade(new BigDecimal("100000"))).isEqualTo(100);
        assertThat(regra.separacao(TipoMaterial.MISTO)).isZero();
        assertThat(regra.separacao(TipoMaterial.PAPEL)).isEqualTo(5);
    }

    @Test
    void niveisUsamLimitesInclusivos() {
        RegraEcoScore regra = regra();
        assertThat(regra.nivel(99)).isEqualTo(NivelSelo.INICIAL);
        assertThat(regra.nivel(100)).isEqualTo(NivelSelo.BRONZE);
        assertThat(regra.nivel(500)).isEqualTo(NivelSelo.PRATA);
        assertThat(regra.nivel(1000)).isEqualTo(NivelSelo.OURO);
        assertThat(regra.proximo(999)).isEqualTo(1);
        assertThat(regra.proximo(1001)).isZero();
    }

    @Test
    void estadosDeMaterialImpedemPularTriagemOuRecontarRecebimento() {
        assertThat(StatusMaterial.AGENDADA.permite(StatusMaterial.COLETADO)).isFalse();
        assertThat(StatusMaterial.AGENDADA.permite(StatusMaterial.TRIAGEM)).isTrue();
        assertThat(StatusMaterial.TRIAGEM.permite(StatusMaterial.COLETADO)).isTrue();
        assertThat(StatusMaterial.COLETADO.permite(StatusMaterial.COMERCIALIZADO)).isTrue();
        assertThat(StatusMaterial.COMERCIALIZADO.permite(StatusMaterial.COLETADO)).isFalse();
        assertThat(UnidadeMaterial.L.aceita(TipoMaterial.METAL)).isFalse();
        assertThat(UnidadeMaterial.L.aceita(TipoMaterial.OLEO)).isTrue();
    }

    private RegraEcoScore regra() {
        RegraEcoScore regra = new RegraEcoScore();
        ReflectionTestUtils.setField(regra, "pontosPorUnidade", new BigDecimal("0.1"));
        ReflectionTestUtils.setField(regra, "limiteQuantidade", 100);
        ReflectionTestUtils.setField(regra, "separacao", 5);
        ReflectionTestUtils.setField(regra, "bronze", 100L);
        ReflectionTestUtils.setField(regra, "prata", 500L);
        ReflectionTestUtils.setField(regra, "ouro", 1000L);
        return regra;
    }
}
