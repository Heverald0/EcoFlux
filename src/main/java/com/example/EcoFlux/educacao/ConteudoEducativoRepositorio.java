package com.example.EcoFlux.educacao;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ConteudoEducativoRepositorio extends JpaRepository<ConteudoEducativo, Long> {
    Page<ConteudoEducativo> findByPublicadoTrue(Pageable pageable);
    List<ConteudoEducativo> findByPublicadoTrueOrderByOrdemAscIdAsc();
    long countByPublicadoTrue();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConteudoEducativo c where c.id = :id")
    Optional<ConteudoEducativo> buscarParaAtualizar(@Param("id") Long id);
}
