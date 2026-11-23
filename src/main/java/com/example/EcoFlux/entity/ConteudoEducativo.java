package com.example.EcoFlux.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_conteudos_educativos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConteudoEducativo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String resumo;

    @Column(name = "corpo_texto", nullable = false, columnDefinition = "TEXT")
    private String corpoTexto;

    @Column(name = "tempo_leitura_minutos", nullable = false)
    private Integer tempoLeituraMinutos;

    @Column(name = "material_relacionado", nullable = false, length = 40)
    private String materialRelacionado;

    @Column(name = "boas_praticas", columnDefinition = "TEXT")
    private String boasPraticas;

    @Column(name = "o_que_evitar", columnDefinition = "TEXT")
    private String oQueEvitar;

    @Column(name = "impacto_destaque", length = 255)
    private String impactoDestaque;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void aoCriar() {
        this.dataCriacao = LocalDateTime.now();
        if (this.ativo == null) this.ativo = true;
        if (this.tempoLeituraMinutos == null) this.tempoLeituraMinutos = 3;
        if (this.materialRelacionado == null || this.materialRelacionado.isBlank()) {
            this.materialRelacionado = "GERAL";
        }
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}