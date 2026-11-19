package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.AmbientalDtos;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.EcoScoreServico;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ecoselo")
@RequiredArgsConstructor
@Validated
public class EcoSeloControlador {
    private final EcoScoreServico scores;
    @GetMapping("/resumo")
    public EcoScoreServico.ResumoSelos resumo(@AuthenticationPrincipal Usuario usuario) {
        return scores.resumoSelos(usuario);
    }

    @GetMapping
    public Page<AmbientalDtos.Certificado> listar(@RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
            @AuthenticationPrincipal Usuario usuario) {
        return scores.listarCertificados(pagina, tamanho, usuario);
    }

    @GetMapping("/instituicoes/{id}/historico")
    public List<AmbientalDtos.Certificado> historico(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return scores.certificados(id, usuario);
    }

    @PostMapping("/instituicoes/{id}/emitir")
    public AmbientalDtos.Certificado emitir(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return scores.renovar(id, usuario);
    }
}
