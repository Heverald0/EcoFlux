package com.example.EcoFlux.exception;

public class ExcecaoConflitoNegocio extends RuntimeException {
    public ExcecaoConflitoNegocio(String mensagem) {
        super(mensagem);
    }
}