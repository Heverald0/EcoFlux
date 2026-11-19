package com.example.EcoFlux.rotas;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParadaRotaRepositorio extends JpaRepository<ParadaRota, Long> {
    boolean existsByColetaId(Long coletaId);
}
