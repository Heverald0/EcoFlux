package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByDocumento(String documento);

    boolean existsByEmail(String email);

    boolean existsByDocumento(String documento);

    List<Usuario> findAllByStatusOrderByDataCriacaoDesc(StatusCadastro status);

    List<Usuario> findAllByOrderByDataCriacaoDesc();
}