package com.example.EcoFlux.controller;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.DashboardServico;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardControlador {
    private final DashboardServico dashboard;

    @GetMapping
    public DashboardServico.Dashboard consultar(@RequestParam(required = false) Long instituicaoId,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            @AuthenticationPrincipal Usuario usuario) {
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        return dashboard.consultar(instituicaoId, inicio == null ? ate.minusMonths(4).withDayOfMonth(1) : inicio, ate, usuario);
    }
}
