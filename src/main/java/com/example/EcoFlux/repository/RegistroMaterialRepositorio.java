package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.RegistroMaterial;
import com.example.EcoFlux.entity.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegistroMaterialRepositorio extends JpaRepository<RegistroMaterial, Long>, JpaSpecificationExecutor<RegistroMaterial> {
    Optional<RegistroMaterial> findByColetaId(Long coletaId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from RegistroMaterial m where m.id = :id")
    Optional<RegistroMaterial> buscarParaAtualizar(@Param("id") Long id);
}
