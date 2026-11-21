package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.response.EcoScoreResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.SustentabilidadeServico;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ecoscore")
@RequiredArgsConstructor
public class EcoScoreControlador {

    private final SustentabilidadeServico sustentabilidadeServico;

    @GetMapping("/meu")
    public ResponseEntity<EcoScoreResposta> obterMeuEcoScore(
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(sustentabilidadeServico.calcularEcoScoreUsuario(usuarioAutenticado));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<EcoScoreResposta>> obterRanking() {
        return ResponseEntity.ok(sustentabilidadeServico.listarRankingEcoScore());
    }
}