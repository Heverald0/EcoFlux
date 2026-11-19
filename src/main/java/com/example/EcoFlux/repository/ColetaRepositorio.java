package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColetaRepositorio extends JpaRepository<Coleta, Long>, JpaSpecificationExecutor<Coleta> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coleta c where c.id = :id")
    Optional<Coleta> buscarParaAtualizar(@Param("id") Long id);
    List<Coleta> findAllByUsuarioSolicitanteOrderByDataHoraAgendamentoDesc(Usuario usuarioSolicitante);
    List<Coleta> findAllByUsuarioSolicitanteAndStatusOrderByDataHoraAgendamentoDesc(Usuario usuarioSolicitante, StatusColeta status);
    List<Coleta> findAllByStatusOrderByDataHoraAgendamentoAsc(StatusColeta status);
    Optional<Coleta> findByIdAndUsuarioSolicitante(Long id, Usuario usuarioSolicitante);
}
