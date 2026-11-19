package com.example.EcoFlux.configuracao;

import com.example.EcoFlux.exception.RespostaErro;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FiltroLimiteAcesso extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final Map<String, Janela> janelas = new HashMap<>();
    @Value("${app.security.auth-limit:30}")
    private int limite;
    @Value("${app.security.auth-window-seconds:60}")
    private long segundos;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requisicao) {
        return !"POST".equals(requisicao.getMethod()) || !(requisicao.getRequestURI().equals("/api/v1/auth/login")
                || requisicao.getRequestURI().equals("/api/v1/auth/cadastro") || requisicao.getRequestURI().equals("/api/v1/usuarios"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        if (!permitir(req.getRemoteAddr(), System.currentTimeMillis())) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(Math.max(1, segundos)));
            res.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(res.getOutputStream(), new RespostaErro(429, "Too Many Requests", "Muitas tentativas. Aguarde antes de tentar novamente.", req.getRequestURI()));
            return;
        }
        chain.doFilter(req, res);
    }

    private synchronized boolean permitir(String endereco, long agora) {
        janelas.entrySet().removeIf(entry -> entry.getValue().fim <= agora);
        Janela janela = janelas.get(endereco);
        if (janela == null) {
            if (janelas.size() >= 10000) return false;
            janela = new Janela(agora + Math.max(1, segundos) * 1000);
            janelas.put(endereco, janela);
        }
        return ++janela.tentativas <= Math.max(1, limite);
    }

    private static final class Janela {
        private final long fim;
        private int tentativas;
        private Janela(long fim) { this.fim = fim; }
    }
}
