package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.PontoColeta;
import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PontoColetaRepositorio extends JpaRepository<PontoColeta, Long> {

    boolean existsByNomeIgnoreCaseAndCidadeIgnoreCase(String nome, String cidade);

    @Query("""
            SELECT DISTINCT p FROM PontoColeta p
            LEFT JOIN p.materiaisAceitos m
            WHERE (:status IS NULL OR p.status = :status)
              AND (:bairro IS NULL OR LOWER(p.bairro) LIKE LOWER(CONCAT('%', :bairro, '%')))
              AND (:material IS NULL OR m = :material)
            ORDER BY p.nome ASC
            """)
    List<PontoColeta> buscarComFiltros(
            @Param("status") StatusPontoColeta status,
            @Param("bairro") String bairro,
            @Param("material") TipoMaterial material
    );
}