package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.AmbientalDtos;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.IndicadoresServico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ecoimpacto")
@RequiredArgsConstructor
@Validated
public class EcoImpactoControlador {
    private final IndicadoresServico indicadores;
    @GetMapping("/ranking")
    public List<IndicadoresServico.ImpactoInstituicao> ranking(@RequestParam LocalDate inicio, @RequestParam LocalDate fim,
            @AuthenticationPrincipal Usuario usuario) {
        return indicadores.ranking(inicio, fim, usuario);
    }

    @PostMapping("/reprocessar")
    public Map<String, Long> reprocessar(@RequestParam(required = false) Long instituicaoId,
            @RequestParam LocalDate inicio, @RequestParam LocalDate fim, @AuthenticationPrincipal Usuario usuario) {
        return indicadores.reprocessar(instituicaoId, inicio, fim, usuario);
    }

    @GetMapping
    public AmbientalDtos.Resumo resumo(@RequestParam(required = false) Long instituicaoId,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            @AuthenticationPrincipal Usuario usuario) {
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        return indicadores.resumo(instituicaoId, inicio == null ? ate.minusMonths(4).withDayOfMonth(1) : inicio, ate, usuario);
    }

    @GetMapping("/fatores")
    public Page<AmbientalDtos.FatorResposta> fatores(@RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return indicadores.fatores(pagina, tamanho);
    }

    @PostMapping("/fatores")
    @ResponseStatus(HttpStatus.CREATED)
    public AmbientalDtos.FatorResposta cadastrarFator(@RequestBody @Valid AmbientalDtos.FatorCadastro requisicao,
            @AuthenticationPrincipal Usuario usuario) {
        return indicadores.cadastrarFator(requisicao, usuario);
    }

    @GetMapping("/metas")
    public List<AmbientalDtos.MetaResposta> metas(@RequestParam Long instituicaoId, @AuthenticationPrincipal Usuario usuario) {
        return indicadores.metas(instituicaoId, usuario);
    }

    @PostMapping("/metas")
    @ResponseStatus(HttpStatus.CREATED)
    public AmbientalDtos.MetaResposta cadastrarMeta(@RequestBody @Valid AmbientalDtos.MetaCadastro requisicao,
            @AuthenticationPrincipal Usuario usuario) {
        return indicadores.cadastrarMeta(requisicao, usuario);
    }
}
