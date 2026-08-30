package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.InstituicaoCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;
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

    @GetMapping("/minhas")
    public ResponseEntity<List<InstituicaoResposta>> listarMinhasInstituicoes(
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        List<InstituicaoResposta> instituicoes = instituicaoServico.listarMinhasInstituicoes(usuarioAutenticado);
        return ResponseEntity.ok(instituicoes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstituicaoResposta> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        InstituicaoResposta resposta = instituicaoServico.buscarPorId(id, usuarioAutenticado);
        return ResponseEntity.ok(resposta);
    }
}