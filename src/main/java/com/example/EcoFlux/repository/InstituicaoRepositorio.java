package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstituicaoRepositorio extends JpaRepository<Instituicao, Long> {
    boolean existsByDocumento(String documento);
    List<Instituicao> findAllByUsuarioResponsavel(Usuario usuarioResponsavel);
    Optional<Instituicao> findByIdAndUsuarioResponsavel(Long id, Usuario usuarioResponsavel);
}