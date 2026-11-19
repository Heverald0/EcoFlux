package com.example.EcoFlux.relatorios;

import com.example.EcoFlux.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "tb_relatorios") @Getter @Setter
public class Relatorio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TipoRelatorio tipo;
    @Column(nullable = false)
    private LocalDate inicio;
    @Column(nullable = false)
    private LocalDate fim;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    @Column(name = "instituicao_id")
    private Long instituicaoId;
    @Column(nullable = false, length = 30)
    private String status = "PENDENTE";
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String dados = "{}";
    @Column(name = "gerado_em", nullable = false)
    private LocalDateTime geradoEm;
    @Column(nullable = false)
    private long exportacoes;
}
