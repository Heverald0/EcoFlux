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
@Table(name = "tb_movimentos_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentoScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private RegistroMaterial material;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id", nullable = true, unique = false)
    private Instituicao instituicao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = false)
    private Usuario usuario;
    @Column(nullable = false)
    private int frequencia;
    @Column(nullable = false)
    private int separacao;
    @Column(nullable = false)
    private int quantidade;
    @Column(nullable = false)
    private int regularidade;
    @Column(nullable = false)
    private int total;
    @Column(nullable = false, length = 50)
    private String regraVersao;
    @Column(nullable = false)
    private LocalDate dataReferencia;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

}
