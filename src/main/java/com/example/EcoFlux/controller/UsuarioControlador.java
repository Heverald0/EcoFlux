package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.UsuarioCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.service.UsuarioServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioControlador {

    private final UsuarioServico usuarioServico;

    @PostMapping
    public ResponseEntity<UsuarioResposta> cadastrar(
            @RequestBody @Valid UsuarioCadastroRequisicao requisicao,
            UriComponentsBuilder construtorUri
    ) {
        UsuarioResposta resposta = usuarioServico.cadastrar(requisicao);
        URI uri = construtorUri.path("/api/v1/usuarios/{id}").buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }
}