package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColetaRepositorio extends JpaRepository<Coleta, Long> {
    List<Coleta> findAllByUsuarioSolicitanteOrderByDataHoraAgendamentoDesc(Usuario usuarioSolicitante);
    List<Coleta> findAllByStatusOrderByDataHoraAgendamentoAsc(StatusColeta status);
    Optional<Coleta> findByIdAndUsuarioSolicitante(Long id, Usuario usuarioSolicitante);
}