package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.example.EcoFlux.entity.enums.StatusInstituicao;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstituicaoRepositorio extends JpaRepository<Instituicao, Long>, JpaSpecificationExecutor<Instituicao> {
    boolean existsByDocumento(String documento);
    @EntityGraph(attributePaths = "usuarioResponsavel")
    List<Instituicao> findAllByUsuarioResponsavel(Usuario usuarioResponsavel);
    @EntityGraph(attributePaths = "usuarioResponsavel")
    List<Instituicao> findAllByUsuarioResponsavelIdIn(List<Long> usuarioIds);
    Optional<Instituicao> findByIdAndUsuarioResponsavel(Long id, Usuario usuarioResponsavel);
    long countByStatus(StatusInstituicao status);
    long countByTipo(TipoInstituicao tipo);
    @EntityGraph(attributePaths = "usuarioResponsavel")
    List<Instituicao> findAllByTipoAndStatus(TipoInstituicao tipo, StatusInstituicao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Instituicao i where i.id = :id")
    Optional<Instituicao> buscarParaAtualizar(@Param("id") Long id);
}
