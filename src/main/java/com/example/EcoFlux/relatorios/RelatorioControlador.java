package com.example.EcoFlux.relatorios;

import com.example.EcoFlux.entity.Usuario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/relatorios") @RequiredArgsConstructor
public class RelatorioControlador {
    private final RelatorioServico servico;
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public RelatorioDtos.Resposta gerar(@RequestBody @Valid RelatorioDtos.Gerar pedido, @AuthenticationPrincipal Usuario usuario) {
        return servico.gerar(pedido, usuario);
    }
    @GetMapping
    public Page<RelatorioDtos.Resposta> listar(@AuthenticationPrincipal Usuario usuario, Pageable pagina) { return servico.listar(usuario, pagina); }
    @GetMapping("/resumo")
    public Map<String, Long> resumo(@AuthenticationPrincipal Usuario usuario) { return servico.resumo(usuario); }
    @GetMapping("/{id}")
    public RelatorioDtos.Detalhe buscar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) { return servico.buscar(id, usuario); }
    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> baixar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ecoflux-relatorio-" + id + ".pdf")
                .cacheControl(CacheControl.noStore()).body(servico.baixar(id, usuario));
    }
}
