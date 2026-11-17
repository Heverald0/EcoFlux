package com.example.EcoFlux.controller;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.service.EventoServico;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/notificacoes") @RequiredArgsConstructor
public class NotificacaoControlador {
    private final EventoServico servico;
    @GetMapping
    public Page<EventoServico.Notificacao> listar(@AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "false") boolean naoLidas, Pageable pageable) {
        return servico.listar(usuario, naoLidas, pageable);
    }
    @GetMapping("/nao-lidas")
    public Map<String, Long> contar(@AuthenticationPrincipal Usuario usuario) {
        return Map.of("total", servico.contarNaoLidas(usuario));
    }
    @PatchMapping("/{id}/lida")
    public EventoServico.Notificacao ler(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servico.ler(id, usuario);
    }
}
