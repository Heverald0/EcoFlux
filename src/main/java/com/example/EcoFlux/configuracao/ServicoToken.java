package com.example.EcoFlux.configuracao;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.EcoFlux.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class ServicoToken {

    @Value("${api.security.token.secret}")
    private String segredo;

    @Value("${api.security.token.expiration-hours}")
    private Integer horasExpiracao;

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(segredo);
            return JWT.create()
                    .withIssuer("ecoflux-api")
                    .withSubject(usuario.getEmail())
                    .withClaim("id", usuario.getId())
                    .withClaim("perfil", usuario.getPerfil().name())
                    .withExpiresAt(obterDataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException excecao) {
            throw new RuntimeException("Erro ao gerar token JWT de autenticacao.", excecao);
        }
    }

    public String validarToken(String token) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(segredo);
            return JWT.require(algoritmo)
                    .withIssuer("ecoflux-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException excecao) {
            return null;
        }
    }

    public Long obterTempoExpiracaoSegundos() {
        return (long) horasExpiracao * 3600;
    }

    private Instant obterDataExpiracao() {
        return LocalDateTime.now().plusHours(horasExpiracao).toInstant(ZoneOffset.of("-03:00"));
    }
}