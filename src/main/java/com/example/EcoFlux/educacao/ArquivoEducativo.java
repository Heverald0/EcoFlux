package com.example.EcoFlux.educacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tb_arquivos_educativos")
@Getter
@Setter
public class ArquivoEducativo {
    @Id
    private Long id;
    @MapsId @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conteudo_id")
    private ConteudoEducativo conteudo;
    @Lob @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] dados;
}
