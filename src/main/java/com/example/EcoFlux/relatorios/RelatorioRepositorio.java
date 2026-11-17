package com.example.EcoFlux.relatorios;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface RelatorioRepositorio extends JpaRepository<Relatorio, Long>, JpaSpecificationExecutor<Relatorio> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Relatorio r where r.id = :id")
    Optional<Relatorio> bloquear(@Param("id") Long id);
}
