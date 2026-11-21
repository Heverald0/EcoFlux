package com.example.EcoFlux.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoMaterial {
    PLASTICO("Plástico", "Garrafas PET, embalagens plásticas, PVC e polietileno", "kg", 1.5),
    PAPEL("Papel e Papelão", "Caixas de papelão, jornais, revistas e papel branco", "kg", 0.9),
    VIDRO("Vidro", "Garrafas, potes e frascos de vidro transparentes ou coloridos", "kg", 0.3),
    METAL("Metal", "Latas de alumínio, aço, cobre e sucatas metálicas", "kg", 4.2),
    ORGANICO("Orgânico", "Resíduos compostáveis de origem vegetal e poda", "kg", 0.5),
    ELETRONICO("Eletrônico (E-Lixo)", "Computadores, celulares, cabos, placas e eletrodomésticos", "kg", 3.8),
    OLEO("Óleo de Cozinha", "Óleo vegetal saturado usado para produção de biodiesel/sabão", "L", 2.5),
    MISTO("Reciclável Misto", "Lote misto de materiais recicláveis secos para triagem", "kg", 1.1);

    private final String nomeExibicao;
    private final String descricao;
    private final String unidadePadrao;
    private final double fatorCarbonoEvitadoKg;
}