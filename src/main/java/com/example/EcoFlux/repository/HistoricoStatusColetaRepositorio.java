package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.HistoricoStatusColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoStatusColetaRepositorio extends JpaRepository<HistoricoStatusColeta, Long> {
    List<HistoricoStatusColeta> findAllByColetaIdOrderByDataRegistroAsc(Long coletaId);
}