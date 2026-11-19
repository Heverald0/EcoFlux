package com.example.EcoFlux.configuracao;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import com.example.EcoFlux.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class ServicoToken {

    @Value("${api.security.token.secret}")
    private String segredo;

    @Value("${api.security.token.expiration-hours}")
    private Integer horasExpiracao;

    @PostConstruct
    void validarConfiguracao() {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET deve conter pelo menos 32 bytes.");
        }
        if (horasExpiracao == null || horasExpiracao < 1 || horasExpiracao > 24) {
            throw new IllegalStateException("A validade do token deve estar entre 1 e 24 horas.");
        }
    }

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(segredo);
            return JWT.create()
                    .withIssuer("ecoflux-api")
                    .withSubject(usuario.getEmail())
                    .withClaim("id", usuario.getId())
                    .withClaim("perfil", usuario.getPerfil().name())
                    .withClaim("versao", usuario.getVersaoToken())
                    .withJWTId(UUID.randomUUID().toString())
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(obterDataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException excecao) {
            throw new RuntimeException("Erro ao gerar token JWT de autenticacao.", excecao);
        }
    }

    public String validarToken(String token) {
        DecodedJWT dados = verificar(token);
        return dados == null ? null : dados.getSubject();
    }

    public DecodedJWT verificar(String token) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(segredo);
            return JWT.require(algoritmo)
                    .withIssuer("ecoflux-api")
                    .build()
                    .verify(token);
        } catch (JWTVerificationException excecao) {
            return null;
        }
    }

    public Long obterTempoExpiracaoSegundos() {
        return (long) horasExpiracao * 3600;
    }

    private Instant obterDataExpiracao() {
        return Instant.now().plus(horasExpiracao, ChronoUnit.HOURS);
    }
}
