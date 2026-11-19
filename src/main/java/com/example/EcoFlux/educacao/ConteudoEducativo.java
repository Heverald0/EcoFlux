package com.example.EcoFlux.educacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_conteudos_educativos")
@Getter
@Setter
public class ConteudoEducativo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String titulo;
    @Column(nullable = false, length = 5000)
    private String descricao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TipoConteudo tipo;
    @Column(name = "url_midia", length = 1500)
    private String urlMidia;
    @Column(nullable = false)
    private boolean publicado;
    @Column(nullable = false)
    private int ordem;
    @Column(nullable = false)
    private boolean destaque;
    @CreationTimestamp @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    @UpdateTimestamp @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
    @Version
    private long versao;
    @OneToMany(mappedBy = "conteudo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<PerguntaQuiz> perguntas = new ArrayList<>();
}
