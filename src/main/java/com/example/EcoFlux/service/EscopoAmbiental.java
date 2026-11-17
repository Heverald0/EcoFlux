package com.example.EcoFlux.service;

import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.exception.ExcecaoRegraNegocio;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class EscopoAmbiental {
    private final InstituicaoRepositorio instituicoes;

    public boolean administrador(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR;
    }

    public Instituicao instituicao(Long id, Usuario usuario) {
        if (id == null) return null;
        Instituicao instituicao = instituicoes.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Instituicao nao encontrada."));
        if (!administrador(usuario) && !instituicao.getUsuarioResponsavel().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Sem permissao para esta instituicao.");
        }
        return instituicao;
    }

    public void periodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim) || ChronoUnit.MONTHS.between(inicio, fim) > 120) {
            throw new ExcecaoRegraNegocio("Informe um periodo valido de ate dez anos.");
        }
    }

    public void exigirAdministrador(Usuario usuario) {
        if (!administrador(usuario)) throw new AccessDeniedException("Operacao exclusiva da administracao.");
    }
}
