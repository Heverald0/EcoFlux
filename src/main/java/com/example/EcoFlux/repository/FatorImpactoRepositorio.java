package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.FatorImpacto;
import com.example.EcoFlux.entity.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FatorImpactoRepositorio extends JpaRepository<FatorImpacto, Long>, JpaSpecificationExecutor<FatorImpacto> {
    Optional<FatorImpacto> findFirstByMaterialAndUnidadeAndVigenciaLessThanEqualOrderByVigenciaDescIdDesc(TipoMaterial material, UnidadeMaterial unidade, LocalDate data);
    boolean existsByMaterialAndUnidadeAndVersao(TipoMaterial material, UnidadeMaterial unidade, String versao);
}
