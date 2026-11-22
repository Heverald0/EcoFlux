package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.response.EcoSeloResposta;
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
@RequestMapping("/api/v1/ecoselos")
@RequiredArgsConstructor
public class EcoSeloControlador {

    private final SustentabilidadeServico sustentabilidadeServico;

    @GetMapping
    public ResponseEntity<List<EcoSeloResposta>> listarSelos(
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        if (usuarioAutenticado != null) {
            return ResponseEntity.ok(sustentabilidadeServico.listarSelosUsuario(usuarioAutenticado));
        }
        return ResponseEntity.ok(sustentabilidadeServico.listarCatalogoSelos());
    }

    @GetMapping("/meus")
    public ResponseEntity<List<EcoSeloResposta>> listarMeusSelos(
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(sustentabilidadeServico.listarSelosUsuario(usuarioAutenticado));
    }
}