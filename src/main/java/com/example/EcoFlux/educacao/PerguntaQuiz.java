package com.example.EcoFlux.educacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_perguntas_quiz")
@Getter
@Setter
public class PerguntaQuiz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conteudo_id", nullable = false)
    private ConteudoEducativo conteudo;
    @Column(nullable = false, length = 1000)
    private String enunciado;
    @Column(nullable = false)
    private int ordem;
    @Column(name = "alternativa_correta", nullable = false)
    private int alternativaCorreta;
    @Column(nullable = false, length = 2000)
    private String explicacao;
    @ElementCollection
    @CollectionTable(name = "tb_alternativas_quiz", joinColumns = @JoinColumn(name = "pergunta_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "texto", nullable = false, length = 1000)
    private List<String> alternativas = new ArrayList<>();
}
