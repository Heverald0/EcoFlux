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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ecoscore")
@RequiredArgsConstructor
@Validated
public class EcoScoreControlador {
    private final EcoScoreServico scores;

    @GetMapping
    public AmbientalDtos.Score consultar(@RequestParam(required = false) Long instituicaoId,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            @AuthenticationPrincipal Usuario usuario) {
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        return scores.consultar(instituicaoId, inicio == null ? ate.minusYears(10) : inicio, ate, usuario);
    }

    @GetMapping("/historico")
    public Page<AmbientalDtos.Movimento> historico(@RequestParam(required = false) Long instituicaoId,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
            @AuthenticationPrincipal Usuario usuario) {
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        return scores.historico(instituicaoId, inicio == null ? ate.minusYears(10) : inicio, ate, pagina, tamanho, usuario);
    }

    @GetMapping("/ranking")
    public List<AmbientalDtos.Ranking> ranking(@RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim) {
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        return scores.ranking(inicio == null ? ate.withDayOfMonth(1) : inicio, ate);
    }
}
