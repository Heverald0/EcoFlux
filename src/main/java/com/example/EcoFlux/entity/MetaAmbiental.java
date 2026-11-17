package com.example.EcoFlux.entity;

import com.example.EcoFlux.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_metas_ambientais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaAmbiental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id", nullable = false, unique = false)
    private Instituicao instituicao;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IndicadorAmbiental indicador;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal alvo;
    @Column(nullable = false)
    private LocalDate inicio;
    @Column(nullable = false)
    private LocalDate fim;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false, unique = false)
    private Usuario autor;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

}
