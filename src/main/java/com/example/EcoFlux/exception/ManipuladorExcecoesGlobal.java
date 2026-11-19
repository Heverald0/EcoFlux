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
@lombok.extern.slf4j.Slf4j
public class ManipuladorExcecoesGlobal {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<RespostaErro> arquivoGrande(Exception excecao, HttpServletRequest requisicao) {
        return ResponseEntity.status(413).body(new RespostaErro(413, "Payload Too Large", "Arquivo excede o limite de 5 MB.", requisicao.getRequestURI()));
    }
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<RespostaErro> acessoNegado(Exception excecao, HttpServletRequest requisicao) {
        return ResponseEntity.status(403).body(new RespostaErro(403, "Forbidden", "Acesso negado.", requisicao.getRequestURI()));
    }

    @ExceptionHandler({ExcecaoRegraNegocio.class, org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.HandlerMethodValidationException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            jakarta.validation.ConstraintViolationException.class})
    public ResponseEntity<RespostaErro> entradaInvalida(Exception excecao, HttpServletRequest requisicao) {
        String mensagem = excecao instanceof ExcecaoRegraNegocio ? excecao.getMessage() : "Requisicao invalida. Verifique os campos e filtros.";
        return ResponseEntity.badRequest().body(new RespostaErro(400, "Bad Request", mensagem, requisicao.getRequestURI()));
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<RespostaErro> conflitoPersistencia(Exception excecao, HttpServletRequest requisicao) {
        return ResponseEntity.status(409).body(new RespostaErro(409, "Conflict",
                "Registro duplicado, em uso ou alterado por outra operacao. Atualize os dados e tente novamente.", requisicao.getRequestURI()));
    }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<RespostaErro> rotaNaoEncontrada(Exception excecao, HttpServletRequest requisicao) {
        return ResponseEntity.status(404).body(new RespostaErro(404, "Not Found", "Rota nao encontrada.", requisicao.getRequestURI()));
    }

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
        log.error("Falha ao processar {} {}", requisicao.getMethod(), requisicao.getRequestURI(), excecao);
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

    @ExceptionHandler(ExcecaoRecursoNaoEncontrado.class)
    public ResponseEntity<RespostaErro> manipularRecursoNaoEncontrado(
            ExcecaoRecursoNaoEncontrado excecao,
            HttpServletRequest requisicao
    ) {
        RespostaErro erro = new RespostaErro(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                excecao.getMessage(),
                requisicao.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(ExcecaoConflitoNegocio.class)
    public ResponseEntity<RespostaErro> manipularConflitoNegocio(
            ExcecaoConflitoNegocio excecao,
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

    
}
