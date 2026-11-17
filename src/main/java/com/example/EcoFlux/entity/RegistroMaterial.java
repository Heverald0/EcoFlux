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
@Table(name = "tb_registros_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMaterial tipoMaterial;
    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantidade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UnidadeMaterial unidade;
    @Column(precision = 14, scale = 3)
    private BigDecimal volume;
    @Column(nullable = false, length = 200)
    private String origem;
    @Column(nullable = false, length = 200)
    private String destino;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusMaterial status;
    @Column(nullable = false)
    private LocalDate dataRegistro;
    @Column()
    private LocalDateTime dataEfetivacao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coleta_id", nullable = true, unique = true)
    private Coleta coleta;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id", nullable = true, unique = false)
    private Instituicao instituicao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false, unique = false)
    private Usuario autor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fator_impacto_id", nullable = true, unique = false)
    private FatorImpacto fatorImpacto;
    @Version
    private Long versao;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

}
