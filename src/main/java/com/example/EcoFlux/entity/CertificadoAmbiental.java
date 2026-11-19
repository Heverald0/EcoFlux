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
@Table(name = "tb_certificados_ambientais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificadoAmbiental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id", nullable = false, unique = false)
    private Instituicao instituicao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NivelSelo nivel;
    @Column(nullable = false)
    private long scoreBase;
    @Column(nullable = false, length = 50)
    private String regraVersao;
    @Column(nullable = false, unique = true, length = 36)
    private String codigo;
    @Column(nullable = false)
    private LocalDate emissao;
    @Column(nullable = false)
    private LocalDate validade;
    @Column(nullable = false, length = 200)
    private String motivo;
    @Column(nullable = false)
    private boolean substituido;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false, unique = false)
    private Usuario autor;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

}
