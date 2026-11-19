package com.example.EcoFlux.educacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "tb_campanhas_educativas")
@Getter
@Setter
public class CampanhaEducativa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String titulo;
    @Column(nullable = false, length = 2000)
    private String descricao;
    @Column(nullable = false)
    private LocalDate inicio;
    @Column(nullable = false)
    private LocalDate fim;
    @Column(name = "url_acao", length = 1500)
    private String urlAcao;
    @Column(nullable = false)
    private boolean publicada;
}
