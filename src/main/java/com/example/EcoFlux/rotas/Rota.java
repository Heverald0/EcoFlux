package com.example.EcoFlux.rotas;

import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_rotas")
@Getter
@Setter
public class Rota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String nome;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cooperativa_id", nullable = false)
    private Instituicao cooperativa;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    @Column(nullable = false)
    private LocalDateTime partida;
    @Column(name = "latitude_origem", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitudeOrigem;
    @Column(name = "longitude_origem", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitudeOrigem;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StatusRota status = StatusRota.PLANEJADA;
    @Column(name = "distancia_km", nullable = false, precision = 12, scale = 3)
    private BigDecimal distanciaKm = BigDecimal.ZERO;
    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;
    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;
    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;
    @CreationTimestamp @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;
    @UpdateTimestamp @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;
    @Version
    private long versao;
    @OneToMany(mappedBy = "rota", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ParadaRota> paradas = new ArrayList<>();
}
