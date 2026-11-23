package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColetaRepositorio extends JpaRepository<Coleta, Long> {
    List<Coleta> findAllByUsuarioSolicitanteOrderByDataHoraAgendamentoDesc(Usuario usuarioSolicitante);
    List<Coleta> findAllByUsuarioSolicitanteAndStatusOrderByDataHoraAgendamentoDesc(Usuario usuarioSolicitante, StatusColeta status);
    List<Coleta> findAllByStatusOrderByDataHoraAgendamentoAsc(StatusColeta status);
    Optional<Coleta> findByIdAndUsuarioSolicitante(Long id, Usuario usuarioSolicitante);

    List<Coleta> findAllByInstituicaoId(Long instituicaoId);

    @Query("""
        SELECT c FROM Coleta c
        WHERE c.motorista.id = :motoristaId
          AND c.status IN :statusAtivos
        ORDER BY c.dataHoraAgendamento ASC
    """)
    List<Coleta> findRotaAtivaDoMotorista(
            @Param("motoristaId") Long motoristaId,
            @Param("statusAtivos") List<StatusColeta> statusAtivos
    );

    @Query("""
        SELECT DISTINCT c FROM Coleta c
        LEFT JOIN c.instituicao inst
        LEFT JOIN c.cooperativa coop
        WHERE c.usuarioSolicitante.id = :usuarioId
           OR inst.usuarioResponsavel.id = :usuarioId
           OR coop.usuarioResponsavel.id = :usuarioId
    """)
    List<Coleta> findAllRelacionadasAoUsuarioId(@Param("usuarioId") Long usuarioId);
}