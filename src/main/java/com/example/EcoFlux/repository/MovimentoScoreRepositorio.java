package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.MovimentoScore;
import com.example.EcoFlux.entity.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimentoScoreRepositorio extends JpaRepository<MovimentoScore, Long>, JpaSpecificationExecutor<MovimentoScore> {
    boolean existsByMaterialId(Long materialId);
    List<MovimentoScore> findByInstituicaoIdOrderByDataCriacaoDesc(Long instituicaoId);
    List<MovimentoScore> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);
}
