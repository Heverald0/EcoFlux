package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.VincularMembroRequisicao;
import com.example.EcoFlux.dto.request.response.MembroInstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.MembroInstituicaoServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instituicoes")
@RequiredArgsConstructor
public class MembroInstituicaoControlador {

    private final MembroInstituicaoServico membroServico;

    @PostMapping("/{instituicaoId}/membros")
    public ResponseEntity<MembroInstituicaoResposta> vincularMembro(
            @PathVariable Long instituicaoId,
            @RequestBody @Valid VincularMembroRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membroServico.vincularMembro(instituicaoId, requisicao, usuarioAutenticado));
    }

    @GetMapping("/{instituicaoId}/membros")
    public ResponseEntity<List<MembroInstituicaoResposta>> listarMembros(
            @PathVariable Long instituicaoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(membroServico.listarMembros(instituicaoId, usuarioAutenticado));
    }

    @DeleteMapping("/{instituicaoId}/membros/{membroId}")
    public ResponseEntity<Void> desvincularMembro(
            @PathVariable Long instituicaoId,
            @PathVariable Long membroId,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        membroServico.desvincularMembro(instituicaoId, membroId, usuarioAutenticado);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/meus-vinculos")
    public ResponseEntity<List<MembroInstituicaoResposta>> listarMeusVinculos(
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(membroServico.listarMeusVinculos(usuarioAutenticado));
    }
}