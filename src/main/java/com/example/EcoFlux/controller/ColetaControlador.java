package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.AtualizarStatusColetaRequisicao;
import com.example.EcoFlux.dto.request.ColetaAgendamentoRequisicao;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.dto.request.response.EventoTimelineColetaResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import com.example.EcoFlux.service.ColetaServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coletas")
@RequiredArgsConstructor
public class ColetaControlador {

    private final ColetaServico coletaServico;

    @PostMapping
    public ResponseEntity<ColetaResposta> agendar(
            @RequestBody @Valid ColetaAgendamentoRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        ColetaResposta resposta = coletaServico.agendar(requisicao, usuarioAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<ColetaResposta>> listarMinhasColetas(
            @RequestParam(required = false) StatusColeta status,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        List<ColetaResposta> coletas = coletaServico.listarMinhasColetas(usuarioAutenticado, status);
        return ResponseEntity.ok(coletas);
    }

    @GetMapping("/operacional")
    public ResponseEntity<List<ColetaResposta>> listarFilaOperacional(
            @RequestParam(required = false) StatusColeta status
    ) {
        List<ColetaResposta> coletas = coletaServico.listarPorStatusOperacional(status);
        return ResponseEntity.ok(coletas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColetaResposta> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        ColetaResposta resposta = coletaServico.buscarPorId(id, usuarioAutenticado);
        return ResponseEntity.ok(resposta);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ColetaResposta> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarStatusColetaRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        ColetaResposta resposta = coletaServico.atualizarStatus(id, requisicao, usuarioAutenticado);
        return ResponseEntity.ok(resposta);
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ColetaResposta> cancelarColeta(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        ColetaResposta resposta = coletaServico.cancelarColeta(id, usuarioAutenticado);
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<EventoTimelineColetaResposta>> consultarTimeline(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        List<EventoTimelineColetaResposta> timeline = coletaServico.consultarTimeline(id, usuarioAutenticado);
        return ResponseEntity.ok(timeline);
    }
}