package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.ConteudoEducativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConteudoEducativoRepositorio extends JpaRepository<ConteudoEducativo, Long> {

    Optional<ConteudoEducativo> findBySlugAndAtivoTrue(String slug);

    boolean existsBySlug(String slug);

    List<ConteudoEducativo> findAllByAtivoTrueOrderByDataCriacaoDesc();

    @Query("""
        SELECT c FROM ConteudoEducativo c
        WHERE c.ativo = true
          AND (UPPER(c.categoria) = UPPER(:filtro) OR UPPER(c.materialRelacionado) = UPPER(:filtro))
        ORDER BY c.dataCriacao DESC
    """)
    List<ConteudoEducativo> findAllAtivosPorCategoriaOuMaterial(@Param("filtro") String filtro);
}