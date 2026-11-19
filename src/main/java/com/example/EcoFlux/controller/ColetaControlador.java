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
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.example.EcoFlux.repository.ColetaRepositorio;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;

@RestController
@RequestMapping("/api/v1/coletas")
@RequiredArgsConstructor
public class ColetaControlador {

    private final ColetaServico coletaServico;
    private final JdbcTemplate jdbc;
    private final ColetaRepositorio repositorio;


    @PutMapping("/{id}")
    @Transactional
    public ColetaResposta editar(@PathVariable Long id, @RequestBody @Valid ColetaAgendamentoRequisicao requisicao,
                                 @AuthenticationPrincipal Usuario usuario) {
        validarSemRota(id, usuario);
        return coletaServico.editar(id, requisicao, usuario);
    }

    private void validarSemRota(Long id, Usuario usuario) {
        coletaServico.buscarPorId(id, usuario);
        repositorio.buscarParaAtualizar(id);
        Long total = jdbc.queryForObject("select count(*) from tb_paradas_rota p join tb_rotas r on r.id = p.rota_id where p.coleta_id = ? and r.status <> 'CONCLUIDA'", Long.class, id);
        if (total != null && total > 0) throw new ExcecaoConflitoNegocio("Atualize esta coleta pela rota vinculada.");
    }

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
            @RequestParam(required = false) StatusColeta status,
            @AuthenticationPrincipal Usuario usuario
    ) {
        List<ColetaResposta> coletas = coletaServico.listarPorStatusOperacional(status, usuario);
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
    @Transactional
    public ResponseEntity<ColetaResposta> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarStatusColetaRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        validarSemRota(id, usuarioAutenticado);
        ColetaResposta resposta = coletaServico.atualizarStatus(id, requisicao, usuarioAutenticado);
        return ResponseEntity.ok(resposta);
    }

    @PatchMapping("/{id}/cancelar")
    @Transactional
    public ResponseEntity<ColetaResposta> cancelarColeta(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        validarSemRota(id, usuarioAutenticado);
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
