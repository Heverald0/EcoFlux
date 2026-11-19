package com.example.EcoFlux.educacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProgressoConteudoRepositorio extends JpaRepository<ProgressoConteudo, Long> {
    Optional<ProgressoConteudo> findByUsuarioIdAndConteudoId(Long usuarioId, Long conteudoId);
    List<ProgressoConteudo> findByUsuarioId(Long usuarioId);
    @Query("select count(distinct p.usuario.id) from ProgressoConteudo p")
    long contarUsuariosEngajados();
}
