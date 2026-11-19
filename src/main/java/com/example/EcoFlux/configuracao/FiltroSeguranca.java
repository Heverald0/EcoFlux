package com.example.EcoFlux.configuracao;

import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.entity.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FiltroSeguranca extends OncePerRequestFilter {

    private final ServicoToken servicoToken;
    private final UsuarioRepositorio usuarioRepositorio;

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeiaFiltros
    ) throws ServletException, IOException {
        String token = recuperarToken(requisicao);

        if (token != null) {
            var dados = servicoToken.verificar(token);
            if (dados != null && dados.getSubject() != null) {
                Usuario usuario = usuarioRepositorio.findByEmail(dados.getSubject()).orElse(null);
                Long versao = dados.getClaim("versao").asLong();
                if (usuario != null && usuario.isEnabled() && usuario.isAccountNonLocked()
                        && versao != null && versao == usuario.getVersaoToken()
                        && usuario.getId().equals(dados.getClaim("id").asLong())) {
                    var autenticacao = new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            usuario.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            }
        }
        cadeiaFiltros.doFilter(requisicao, resposta);
    }

    private String recuperarToken(HttpServletRequest requisicao) {
        String cabecalhoAutorizacao = requisicao.getHeader("Authorization");
        if (cabecalhoAutorizacao == null || !cabecalhoAutorizacao.startsWith("Bearer ")) {
            return null;
        }
        return cabecalhoAutorizacao.substring(7).trim();
    }
}
