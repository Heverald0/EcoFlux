package com.example.EcoFlux.controller;

import com.example.EcoFlux.dto.request.AlterarUsuarioRequisicao;
import com.example.EcoFlux.dto.request.AnalisarInstituicaoRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.dto.request.response.AdministracaoUsuarioResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.service.AdministracaoServico;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdministracaoControlador {
    private final AdministracaoServico servico;

    @GetMapping("/usuarios")
    public Page<AdministracaoUsuarioResposta> usuarios(@RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusUsuario status, @RequestParam(required = false) PerfilUsuario perfil,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return servico.usuarios(busca, status, perfil, pagina, tamanho);
    }

    @PatchMapping("/usuarios/{id}")
    public UsuarioResposta alterarUsuario(@PathVariable Long id, @Valid @RequestBody AlterarUsuarioRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuario) {
        return servico.alterarUsuario(id, requisicao, usuario);
    }

    @GetMapping("/instituicoes")
    public Page<InstituicaoResposta> instituicoes(@RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusInstituicao status, @RequestParam(required = false) TipoInstituicao tipo,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "20") int tamanho) {
        return servico.instituicoes(busca, status, tipo, pagina, tamanho);
    }

    @PatchMapping("/instituicoes/{id}/status")
    public InstituicaoResposta analisar(@PathVariable Long id, @Valid @RequestBody AnalisarInstituicaoRequisicao requisicao,
            @AuthenticationPrincipal Usuario usuario) {
        return servico.analisarInstituicao(id, requisicao, usuario);
    }

    @GetMapping("/resumo")
    public Map<String, Long> resumo() { return servico.resumo(); }

    @GetMapping("/permissoes")
    public Map<PerfilUsuario, List<String>> permissoes() { return servico.permissoes(); }
}
