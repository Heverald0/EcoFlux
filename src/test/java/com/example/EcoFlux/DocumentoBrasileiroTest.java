package com.example.EcoFlux;

import com.example.EcoFlux.validacao.DocumentoBrasileiro;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentoBrasileiroTest {
    @Test
    void validaCpfECnpjNumericoEAlfanumerico() {
        assertThat(DocumentoBrasileiro.cpfValido("529.982.247-25")).isTrue();
        assertThat(DocumentoBrasileiro.cnpjValido("11.222.333/0001-81")).isTrue();
        assertThat(DocumentoBrasileiro.cnpjValido("12.ABC.345/01DE-35")).isTrue();
        assertThat(DocumentoBrasileiro.cnpjValido("12.abc.345/01de-35")).isTrue();
    }

    @Test
    void rejeitaDocumentosRepetidosDigitosErradosECaracteresEstranhos() {
        assertThat(DocumentoBrasileiro.cpfValido("11111111111")).isFalse();
        assertThat(DocumentoBrasileiro.cpfValido("52998224726")).isFalse();
        assertThat(DocumentoBrasileiro.cnpjValido("00000000000000")).isFalse();
        assertThat(DocumentoBrasileiro.cnpjValido("11222333000182")).isFalse();
        assertThat(DocumentoBrasileiro.cnpjValido("12.ABC.345/01DE-36")).isFalse();
        assertThat(DocumentoBrasileiro.cnpjValido("11$222333000181")).isFalse();
    }
}
