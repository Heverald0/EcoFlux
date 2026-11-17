package com.example.EcoFlux.educacao;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TentativaQuizRepositorio extends JpaRepository<TentativaQuiz, Long> {
    boolean existsByConteudoId(Long conteudoId);
    long countByUsuarioIdAndConteudoId(Long usuarioId, Long conteudoId);
}
