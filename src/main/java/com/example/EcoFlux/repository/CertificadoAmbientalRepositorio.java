package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.CertificadoAmbiental;
import com.example.EcoFlux.entity.enums.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CertificadoAmbientalRepositorio extends JpaRepository<CertificadoAmbiental, Long>, JpaSpecificationExecutor<CertificadoAmbiental> {
    Optional<CertificadoAmbiental> findFirstByInstituicaoIdOrderByDataCriacaoDescIdDesc(Long instituicaoId);
    List<CertificadoAmbiental> findByInstituicaoIdOrderByDataCriacaoDescIdDesc(Long instituicaoId);
}
