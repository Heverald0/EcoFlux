package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.MetaAmbiental;
import com.example.EcoFlux.entity.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MetaAmbientalRepositorio extends JpaRepository<MetaAmbiental, Long>, JpaSpecificationExecutor<MetaAmbiental> {
    List<MetaAmbiental> findByInstituicaoIdOrderByFimAsc(Long instituicaoId);
}
