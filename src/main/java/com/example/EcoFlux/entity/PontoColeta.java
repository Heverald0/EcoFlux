package com.example.EcoFlux.entity;

import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_pontos_coleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class PontoColeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "nome_responsavel", nullable = false, length = 120)
    private String nomeResponsavel;

    @Column(name = "telefone_contato", length = 20)
    private String telefoneContato;

    @Column(name = "horario_funcionamento", nullable = false, length = 120)
    private String horarioFuncionamento;

    @Column(nullable = false, length = 150)
    private String logradouro;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(nullable = false, length = 100)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String estado;

    @Column(nullable = false, length = 10)
    private String cep;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPontoColeta status;

    @Column(name = "motivo_status", length = 255)
    private String motivoStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "tb_ponto_coleta_materiais",
            joinColumns = @JoinColumn(name = "ponto_coleta_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_material", nullable = false, length = 50)
    @Builder.Default
    private Set<TipoMaterial> materiaisAceitos = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_cadastro_id", nullable = false)
    private Usuario usuarioCadastro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
}