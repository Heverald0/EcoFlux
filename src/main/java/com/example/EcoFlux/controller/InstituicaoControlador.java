package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.InstituicaoCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.InstituicaoServico;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.dto.request.response.CooperativaResumo;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.entity.enums.StatusInstituicao;
import org.springframework.transaction.annotation.Transactional;
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
    private final InstituicaoRepositorio instituicoes;

    @GetMapping("/cooperativas")
    @Transactional(readOnly = true)
    public List<CooperativaResumo> cooperativas() {
        return instituicoes.findAllByTipoAndStatus(TipoInstituicao.COOPERATIVA, StatusInstituicao.ATIVO).stream()
                .map(i -> new CooperativaResumo(i.getId(), i.getNome(), i.getCidade(), i.getEstado())).toList();
    }

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
