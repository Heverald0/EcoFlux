package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.MaterialDtos;
import com.example.EcoFlux.dto.AmbientalDtos;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/materiais")
@RequiredArgsConstructor
@Validated
public class MaterialControlador {
    private final MaterialServico materiais;
    private final IndicadoresServico indicadores;

    public record Catalogo(TipoMaterial material, List<UnidadeMaterial> unidades) {}

    @GetMapping("/catalogo")
    public List<Catalogo> catalogo() {
        return Arrays.stream(TipoMaterial.values()).map(tipo -> new Catalogo(tipo,
                Arrays.stream(UnidadeMaterial.values()).filter(u -> u.aceita(tipo)).toList())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDtos.Resposta cadastrar(@RequestBody @Valid MaterialDtos.Cadastro requisicao,
            @AuthenticationPrincipal Usuario usuario) {
        return materiais.cadastrar(requisicao, usuario);
    }

    @GetMapping
    public Page<MaterialDtos.Resposta> listar(@RequestParam(required = false) Long instituicaoId,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            @RequestParam(required = false) TipoMaterial material, @RequestParam(required = false) StatusMaterial status,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho,
            @AuthenticationPrincipal Usuario usuario) {
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        return materiais.listar(instituicaoId, inicio == null ? ate.minusMonths(4).withDayOfMonth(1) : inicio,
                ate, material, status, pagina, tamanho, usuario);
    }

    @GetMapping("/resumo")
    public AmbientalDtos.Resumo resumo(@RequestParam(required = false) Long instituicaoId,
            @RequestParam(required = false) LocalDate inicio, @RequestParam(required = false) LocalDate fim,
            @AuthenticationPrincipal Usuario usuario) {
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        return indicadores.resumo(instituicaoId, inicio == null ? ate.minusMonths(4).withDayOfMonth(1) : inicio, ate, usuario);
    }

    @PatchMapping("/{id}/status")
    public MaterialDtos.Resposta status(@PathVariable Long id, @RequestBody @Valid MaterialDtos.AlterarStatus requisicao,
            @AuthenticationPrincipal Usuario usuario) {
        return materiais.alterarStatus(id, requisicao.status(), usuario);
    }
}
