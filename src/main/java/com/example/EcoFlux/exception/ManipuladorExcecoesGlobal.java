package com.example.EcoFlux.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ManipuladorExcecoesGlobal {

    @ExceptionHandler(EmailJaCadastradoExcecao.class)
    public ResponseEntity<RespostaErro> manipularEmailJaCadastrado(
            EmailJaCadastradoExcecao excecao,
            HttpServletRequest requisicao
    ) {
        RespostaErro erro = new RespostaErro(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                excecao.getMessage(),
                requisicao.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> manipularErrosValidacao(
            MethodArgumentNotValidException excecao,
            HttpServletRequest requisicao
    ) {
        Map<String, String> erros = new HashMap<>();
        for (FieldError erroCampo : excecao.getBindingResult().getFieldErrors()) {
            erros.put(erroCampo.getField(), erroCampo.getDefaultMessage());
        }

        RespostaErro erro = new RespostaErro(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Falha na validação dos campos informados.",
                requisicao.getRequestURI(),
                erros
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> manipularExcecaoGenerica(
            Exception excecao,
            HttpServletRequest requisicao
    ) {
        RespostaErro erro = new RespostaErro(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocorreu um erro interno inesperado no servidor.",
                requisicao.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<RespostaErro> manipularFalhaAutenticacao(
            org.springframework.security.core.AuthenticationException excecao,
            HttpServletRequest requisicao
    ) {
        RespostaErro erro = new RespostaErro(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Credenciais invalidas. Verifique seu e-mail e senha.",
                requisicao.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erro);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RespostaErro> manipularMetodoNaoSuportado(
            org.springframework.web.HttpRequestMethodNotSupportedException excecao,
            HttpServletRequest requisicao
    ) {
        RespostaErro erro = new RespostaErro(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                String.format("Metodo HTTP '%s' nao e suportado para esta rota. Metodos permitidos: %s", 
                        excecao.getMethod(), java.util.Arrays.toString(excecao.getSupportedMethods())),
                requisicao.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(erro);
    }

    
}