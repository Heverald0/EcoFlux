package com.example.EcoFlux.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_eventos")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Evento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 150)
    private String chave;
    @Column(nullable = false, length = 80)
    private String tipo;
    @Column(nullable = false, length = 80)
    private String entidade;
    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destinatario_id")
    private Usuario destinatario;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;
    @Column(nullable = false, length = 1000)
    private String mensagem;
    @CreationTimestamp @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    @Column(name = "data_leitura")
    private LocalDateTime dataLeitura;
}
