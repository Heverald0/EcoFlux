package com.example.EcoFlux.educacao;

import com.example.EcoFlux.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_tentativas_quiz")
@Getter
@Setter
public class TentativaQuiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conteudo_id", nullable = false)
    private ConteudoEducativo conteudo;
    @Column(nullable = false)
    private int acertos;
    @Column(nullable = false)
    private int total;
    @Column(nullable = false)
    private boolean aprovado;
    @Column(name = "respondido_em", nullable = false)
    private LocalDateTime respondidoEm;
}
