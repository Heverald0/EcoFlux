package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.AtualizarStatusCadastroRequisicao;
import com.example.EcoFlux.dto.request.InstituicaoCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.service.InstituicaoServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/instituicoes")
@RequiredArgsConstructor
public class InstituicaoControlador {

    private final InstituicaoServico instituicaoServico;

    @PostMapping
    public ResponseEntity<InstituicaoResposta> cadastrar(
            @RequestBody @Valid InstituicaoCadastroRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        InstituicaoResposta resposta = instituicaoServico.cadastrar(requisicao, usuarioAutenticado);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<InstituicaoResposta>> listar(
            @RequestParam(required = false) TipoInstituicao tipo,
            @RequestParam(required = false) StatusCadastro status
    ) {
        return ResponseEntity.ok(instituicaoServico.listarComFiltros(tipo, status));
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<InstituicaoResposta>> listarMinhasInstituicoes(
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(instituicaoServico.listarMinhasInstituicoes(usuarioAutenticado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstituicaoResposta> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(instituicaoServico.buscarPorId(id, usuarioAutenticado));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InstituicaoResposta> atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarStatusCadastroRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        return ResponseEntity.ok(instituicaoServico.atualizarStatus(id, requisicao, usuarioAutenticado));
    }
}