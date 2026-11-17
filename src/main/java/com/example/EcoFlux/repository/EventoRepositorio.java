package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface EventoRepositorio extends JpaRepository<Evento, Long> {
    Optional<Evento> findByChave(String chave);
    Page<Evento> findByDestinatarioIdOrderByDataCriacaoDesc(Long id, Pageable pageable);
    Page<Evento> findByDestinatarioIdAndDataLeituraIsNullOrderByDataCriacaoDesc(Long id, Pageable pageable);
    long countByDestinatarioIdAndDataLeituraIsNull(Long id);
    Optional<Evento> findByIdAndDestinatarioId(Long id, Long destinatarioId);
}
