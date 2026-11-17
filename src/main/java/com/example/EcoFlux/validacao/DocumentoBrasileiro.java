package com.example.EcoFlux.validacao;

import java.util.Locale;

public final class DocumentoBrasileiro {
    private DocumentoBrasileiro() {}

    public static String normalizar(String documento) {
        return documento == null ? "" : documento.trim().toUpperCase(Locale.ROOT).replaceAll("[. /-]", "");
    }

    public static boolean cpfValido(String valor) {
        String cpf = normalizar(valor);
        if (!cpf.matches("[0-9]{11}") || cpf.chars().distinct().count() == 1) return false;
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            int digito = 11 - soma % 11;
            if (cpf.charAt(tamanho) - '0' != (digito > 9 ? 0 : digito)) return false;
        }
        return true;
    }

    public static boolean cnpjValido(String valor) {
        String cnpj = normalizar(valor);
        if (!cnpj.matches("[A-Z0-9]{12}[0-9]{2}") || cnpj.chars().distinct().count() == 1) return false;
        for (int tamanho = 12; tamanho <= 13; tamanho++) {
            int soma = 0;
            int peso = 2;
            for (int i = tamanho - 1; i >= 0; i--) {
                soma += (cnpj.charAt(i) - 48) * peso;
                peso = peso == 9 ? 2 : peso + 1;
            }
            int resto = soma % 11;
            if (cnpj.charAt(tamanho) - '0' != (resto < 2 ? 0 : 11 - resto)) return false;
        }
        return true;
    }
}
