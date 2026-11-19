package com.example.EcoFlux.rotas;

import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.PontoColeta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_paradas_rota")
@Getter
@Setter
public class ParadaRota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rota_id", nullable = false)
    private Rota rota;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coleta_id", nullable = false, unique = true)
    private Coleta coleta;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ponto_id", nullable = false)
    private PontoColeta ponto;
    @Column(nullable = false)
    private int ordem;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private StatusParada status = StatusParada.PENDENTE;
    @Column(name = "distancia_trecho_km", nullable = false, precision = 12, scale = 3)
    private BigDecimal distanciaTrechoKm;
    @Column(nullable = false)
    private LocalDateTime previsao;
    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;
    @Column(length = 255)
    private String motivo;
}
