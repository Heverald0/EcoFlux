package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.ConteudoEducativoRequisicao;
import com.example.EcoFlux.dto.request.response.ConteudoEducativoResposta;
import com.example.EcoFlux.dto.request.response.GuiaTriagemMaterialResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.ConteudoEducativoServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/educativo")
@RequiredArgsConstructor
public class ConteudoEducativoControlador {

    private final ConteudoEducativoServico conteudoEducativoServico;

    @GetMapping("/artigos")
    public ResponseEntity<List<ConteudoEducativoResposta>> listarArtigos(
            @RequestParam(required = false) String categoria
    ) {
        return ResponseEntity.ok(conteudoEducativoServico.listarArtigos(categoria));
    }

    @GetMapping("/artigos/{slug}")
    public ResponseEntity<ConteudoEducativoResposta> buscarPorSlug(@PathVariable String slug) {
        return ResponseEntity.ok(conteudoEducativoServico.buscarPorSlug(slug));
    }

    @GetMapping("/guia-triagem")
    public ResponseEntity<List<GuiaTriagemMaterialResposta>> listarGuiaTriagem() {
        return ResponseEntity.ok(conteudoEducativoServico.listarGuiaTriagem());
    }

    @PostMapping("/artigos")
    public ResponseEntity<ConteudoEducativoResposta> criarArtigo(
            @RequestBody @Valid ConteudoEducativoRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conteudoEducativoServico.criarArtigo(requisicao, usuarioAutenticado));
    }

    @PutMapping("/artigos/{id}")
    public ResponseEntity<ConteudoEducativoResposta> atualizarArtigo(
            @PathVariable Long id,
            @RequestBody @Valid ConteudoEducativoRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(conteudoEducativoServico.atualizarArtigo(id, requisicao, usuarioAutenticado));
    }

    @PatchMapping("/artigos/{id}/status")
    public ResponseEntity<Void> alterarStatusAtivo(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        conteudoEducativoServico.alterarStatusAtivo(id, ativo, usuarioAutenticado);
        return ResponseEntity.noContent().build();
    }
}