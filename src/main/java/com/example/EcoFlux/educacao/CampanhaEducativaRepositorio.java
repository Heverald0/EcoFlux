package com.example.EcoFlux.educacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface CampanhaEducativaRepositorio extends JpaRepository<CampanhaEducativa, Long> {
    Page<CampanhaEducativa> findByPublicadaTrueAndInicioLessThanEqualAndFimGreaterThanEqual(
            LocalDate inicio, LocalDate fim, Pageable pageable);
}
