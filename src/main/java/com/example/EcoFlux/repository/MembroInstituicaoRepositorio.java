package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.MembroInstituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PapelMembroInstituicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembroInstituicaoRepositorio extends JpaRepository<MembroInstituicao, Long> {

    Optional<MembroInstituicao> findByInstituicaoAndUsuario(Instituicao instituicao, Usuario usuario);

    boolean existsByInstituicaoIdAndUsuarioIdAndAtivoTrue(Long instituicaoId, Long usuarioId);

    boolean existsByUsuarioIdAndPapelAndAtivoTrue(Long usuarioId, PapelMembroInstituicao papel);

    List<MembroInstituicao> findAllByInstituicaoAndAtivoTrueOrderByDataVinculoDesc(Instituicao instituicao);

    List<MembroInstituicao> findAllByUsuarioAndAtivoTrue(Usuario usuario);
}