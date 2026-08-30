package com.example.EcoFlux.controller;

import com.example.EcoFlux.configuracao.ServicoToken;
import com.example.EcoFlux.dto.request.LoginRequisicao;
import com.example.EcoFlux.dto.request.response.TokenResposta;
import com.example.EcoFlux.entity.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AutenticacaoControlador {

    private final AuthenticationManager gerenciadorAutenticacao;
    private final ServicoToken servicoToken;

    @PostMapping("/login")
    public ResponseEntity<TokenResposta> autenticar(@RequestBody @Valid LoginRequisicao requisicao) {
        var dadosAutenticacao = new UsernamePasswordAuthenticationToken(
                requisicao.email().trim().toLowerCase(),
                requisicao.senha()
        );

        Authentication autenticacao = gerenciadorAutenticacao.authenticate(dadosAutenticacao);
        Usuario usuario = (Usuario) autenticacao.getPrincipal();

        String tokenJWT = servicoToken.gerarToken(usuario);
        Long expiraEm = servicoToken.obterTempoExpiracaoSegundos();

        return ResponseEntity.ok(new TokenResposta(tokenJWT, expiraEm));
    }
}