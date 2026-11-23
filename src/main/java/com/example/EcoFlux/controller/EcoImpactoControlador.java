package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.response.EcoImpactoResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.SustentabilidadeServico;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ecoimpacto")
@RequiredArgsConstructor
public class EcoImpactoControlador {

    private final SustentabilidadeServico sustentabilidadeServico;

    @GetMapping
    public ResponseEntity<EcoImpactoResposta> obterImpactoGlobal() {
        return ResponseEntity.ok(sustentabilidadeServico.calcularImpactoGlobal());
    }

    @GetMapping("/meu")
    public ResponseEntity<EcoImpactoResposta> obterMeuImpacto(
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(sustentabilidadeServico.calcularImpactoUsuario(usuarioAutenticado));
    }

    @GetMapping("/instituicao/{instituicaoId}")
    public ResponseEntity<EcoImpactoResposta> obterImpactoInstituicao(
            @PathVariable Long instituicaoId,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(sustentabilidadeServico.calcularImpactoInstituicao(instituicaoId, usuarioAutenticado));
    }
}