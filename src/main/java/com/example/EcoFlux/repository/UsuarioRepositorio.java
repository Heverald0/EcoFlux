package com.example.EcoFlux.repository;

import com.example.EcoFlux.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusUsuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepositorio extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
    boolean existsByDocumento(String documento);
    boolean existsByPerfil(PerfilUsuario perfil);
    long countByStatus(StatusUsuario status);
    long countByPerfil(PerfilUsuario perfil);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaAtualizar(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.versaoToken = u.versaoToken + 1 where u.id = :id")
    int revogarSessoes(@Param("id") Long id);
}
