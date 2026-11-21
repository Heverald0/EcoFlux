package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.AtualizarStatusCadastroRequisicao;
import com.example.EcoFlux.dto.request.UsuarioCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.service.UsuarioServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

    @GetMapping("/me")
    public ResponseEntity<UsuarioResposta> perfilAtual(@AuthenticationPrincipal Usuario usuarioAutenticado) {
        return ResponseEntity.ok(UsuarioResposta.aPartirDaEntidade(usuarioAutenticado));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResposta>> listar(
            @RequestParam(required = false) StatusCadastro status
    ) {
        return ResponseEntity.ok(usuarioServico.listar(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioServico.buscarPorId(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UsuarioResposta> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarStatusCadastroRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(usuarioServico.atualizarStatus(id, requisicao, usuarioAutenticado));
    }
}