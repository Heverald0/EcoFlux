package com.example.EcoFlux.educacao;

import com.example.EcoFlux.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_progresso_conteudo", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "conteudo_id"}))
@Getter
@Setter
public class ProgressoConteudo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conteudo_id", nullable = false)
    private ConteudoEducativo conteudo;
    @Column(nullable = false)
    private int percentual;
    @Column(name = "iniciado_em", nullable = false)
    private LocalDateTime iniciadoEm;
    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;
}
