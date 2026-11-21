package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.AtualizarStatusCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.service.InstituicaoServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cooperativas")
@RequiredArgsConstructor
public class CooperativaControlador {

    private final InstituicaoServico instituicaoServico;

    @GetMapping
    public ResponseEntity<List<InstituicaoResposta>> listarCooperativas(
            @RequestParam(required = false) StatusCadastro status
    ) {
        return ResponseEntity.ok(instituicaoServico.listarComFiltros(TipoInstituicao.COOPERATIVA, status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InstituicaoResposta> atualizarStatusCooperativa(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarStatusCadastroRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(instituicaoServico.atualizarStatus(id, requisicao, usuarioAutenticado));
    }
}