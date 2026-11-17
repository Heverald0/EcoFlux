package com.example.EcoFlux.rotas;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RotaRepositorio extends JpaRepository<Rota, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rota r where r.id = :id")
    Optional<Rota> buscarParaAtualizar(@Param("id") Long id);

    @Query("select r from Rota r where (:admin = true or r.cooperativa.usuarioResponsavel.id = :usuarioId) and (:status is null or r.status = :status)")
    Page<Rota> listar(@Param("usuarioId") Long usuarioId, @Param("admin") boolean admin,
                     @Param("status") StatusRota status, Pageable pageable);
}
