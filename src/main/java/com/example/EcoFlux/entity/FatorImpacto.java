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
@Table(name = "tb_fatores_impacto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatorImpacto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMaterial material;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UnidadeMaterial unidade;
    @Column(nullable = false, length = 50)
    private String versao;
    @Column(nullable = false, length = 1000)
    private String fonte;
    @Column(nullable = false)
    private LocalDate vigencia;
    @Column(name = "co2_kg", nullable = false, precision = 18, scale = 6)
    private BigDecimal co2Kg;
    @Column(name = "agua_l", nullable = false, precision = 18, scale = 6)
    private BigDecimal aguaL;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal energiaKwh;
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal arvores;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false, unique = false)
    private Usuario autor;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

}
