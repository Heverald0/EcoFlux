package com.example.EcoFlux.entity;

import com.example.EcoFlux.entity.enums.StatusColeta;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_coletas_historico_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class HistoricoStatusColeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coleta_id", nullable = false)
    private Coleta coleta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 50)
    private StatusColeta statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 50)
    private StatusColeta statusNovo;

    @Column(length = 255)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_autor_id", nullable = false)
    private Usuario usuarioAutor;

    @CreationTimestamp
    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;
}