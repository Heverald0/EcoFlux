package com.example.EcoFlux.rotas;

import com.example.EcoFlux.entity.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rotas")
@RequiredArgsConstructor
public class RotaControlador {
    private final RotaServico servico;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public RotaResposta planejar(@RequestBody @Valid RotaRequisicoes.Planejar pedido,
                                @AuthenticationPrincipal Usuario usuario) {
        return servico.planejar(pedido, usuario);
    }

    @GetMapping
    public Page<RotaResposta> listar(@AuthenticationPrincipal Usuario usuario,
                                    @RequestParam(required = false) StatusRota status,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    @RequestParam(defaultValue = "20") int tamanho) {
        return servico.listar(usuario, status, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public RotaResposta buscar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servico.buscar(id, usuario);
    }

    @PostMapping("/{id}/otimizar")
    public RotaResposta otimizar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servico.otimizar(id, usuario);
    }

    @PostMapping("/{id}/iniciar")
    public RotaResposta iniciar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servico.iniciar(id, usuario);
    }

    @PostMapping("/{id}/paradas/{paradaId}/concluir")
    public RotaResposta concluir(@PathVariable Long id, @PathVariable Long paradaId,
                                @RequestBody @Valid RotaRequisicoes.Concluir pedido,
                                @AuthenticationPrincipal Usuario usuario) {
        return servico.concluirParada(id, paradaId, pedido, usuario);
    }

    @PostMapping("/{id}/paradas/{paradaId}/falhar")
    public RotaResposta falhar(@PathVariable Long id, @PathVariable Long paradaId,
                              @RequestBody @Valid RotaRequisicoes.Falhar pedido,
                              @AuthenticationPrincipal Usuario usuario) {
        return servico.falharParada(id, paradaId, pedido, usuario);
    }
}
