package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.AlterarStatusPontoRequisicao;
import com.example.EcoFlux.dto.request.PontoColetaRequisicao;
import com.example.EcoFlux.dto.request.response.PontoColetaResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import com.example.EcoFlux.service.PontoColetaServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pontos-coleta")
@RequiredArgsConstructor
public class PontoColetaControlador {

    private final PontoColetaServico pontoColetaServico;

    @PostMapping
    public ResponseEntity<PontoColetaResposta> cadastrar(
            @RequestBody @Valid PontoColetaRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        PontoColetaResposta resposta = pontoColetaServico.cadastrar(requisicao, usuarioAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<PontoColetaResposta>> listar(
            @RequestParam(required = false) StatusPontoColeta status,
            @RequestParam(required = false) String bairro,
            @RequestParam(required = false) TipoMaterial material
    ) {
        return ResponseEntity.ok(pontoColetaServico.listarComFiltros(status, bairro, material));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PontoColetaResposta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pontoColetaServico.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PontoColetaResposta> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid PontoColetaRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(pontoColetaServico.atualizar(id, requisicao, usuarioAutenticado));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PontoColetaResposta> alterarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AlterarStatusPontoRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(pontoColetaServico.alterarStatus(id, requisicao, usuarioAutenticado));
    }
}