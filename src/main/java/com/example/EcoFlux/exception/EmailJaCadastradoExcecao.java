package com.example.EcoFlux.exception;

public class EmailJaCadastradoExcecao extends ExcecaoRegraNegocio {
    public EmailJaCadastradoExcecao(String email) {
        super(String.format("O e-mail '%s' já está cadastrado no sistema.", email));
    }
}