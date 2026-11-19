package com.example.EcoFlux.controller;

import com.example.EcoFlux.entity.Evento;
import com.example.EcoFlux.repository.EventoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController @RequestMapping("/api/v1/admin/auditoria") @RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AuditoriaControlador {
    private final EventoRepositorio eventos;
    @GetMapping @Transactional(readOnly = true)
    public Page<Auditoria> listar(Pageable pagina) {
        return eventos.findAll(PageRequest.of(pagina.getPageNumber(), Math.min(pagina.getPageSize(), 100),
                Sort.by(Sort.Direction.DESC, "dataCriacao", "id"))).map(Auditoria::de);
    }
    public record Auditoria(Long id, String tipo, String entidade, Long entidadeId, Long autorId,
                            Long instituicaoId, String mensagem, LocalDateTime dataCriacao) {
        public static Auditoria de(Evento e) {
            return new Auditoria(e.getId(), e.getTipo(), e.getEntidade(), e.getEntidadeId(), e.getAutor().getId(),
                    e.getInstituicao() == null ? null : e.getInstituicao().getId(), e.getMensagem(), e.getDataCriacao());
        }
    }
}
