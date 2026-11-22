package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.response.EcoImpactoResposta;
import com.example.EcoFlux.dto.request.response.EcoScoreResposta;
import com.example.EcoFlux.dto.request.response.EcoSeloResposta;
import com.example.EcoFlux.dto.request.response.ImpactoPorMaterialResposta;
import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import com.example.EcoFlux.repository.ColetaRepositorio;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.service.SustentabilidadeServico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SustentabilidadeServicoImpl implements SustentabilidadeServico {

    private final ColetaRepositorio coletaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    // Fatores médios ambientais por kg reciclado
    private static final BigDecimal KG_CO2_POR_ARVORE_ANO = new BigDecimal("22.00");
    private static final BigDecimal KWH_ECONOMIZADO_POR_KG = new BigDecimal("5.30");
    private static final BigDecimal LITROS_AGUA_POR_KG = new BigDecimal("28.00");

    @Override
    @Transactional(readOnly = true)
    public EcoImpactoResposta calcularImpactoGlobal() {
        List<Coleta> concluidas = filtrarColetasConcluidas(coletaRepositorio.findAll());
        return montarImpacto(concluidas);
    }

    @Override
    @Transactional(readOnly = true)
    public EcoImpactoResposta calcularImpactoUsuario(Usuario usuario) {
        List<Coleta> concluidas = filtrarColetasConcluidas(
                coletaRepositorio.findAllRelacionadasAoUsuarioId(usuario.getId())
        );
        return montarImpacto(concluidas);
    }

    @Override
    @Transactional(readOnly = true)
    public EcoScoreResposta calcularEcoScoreUsuario(Usuario usuario) {
        List<Coleta> concluidas = filtrarColetasConcluidas(
                coletaRepositorio.findAllRelacionadasAoUsuarioId(usuario.getId())
        );

        BigDecimal totalKg = concluidas.stream()
                .map(Coleta::getQuantidadeEstimadaKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int diversidadeMateriais = (int) concluidas.stream()
                .map(Coleta::getTipoMaterial)
                .distinct()
                .count();

        // Fórmula EcoScore: 10 pts/kg + 50 pts/coleta concluída + 100 pts/material distinto
        int pontosPeso = totalKg.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP).intValue();
        int pontosColetas = concluidas.size() * 50;
        int pontosDiversidade = diversidadeMateriais * 100;
        int pontuacaoTotal = pontosPeso + pontosColetas + pontosDiversidade;

        String nivel;
        int pisoNivel;
        int tetoNivel;

        if (pontuacaoTotal < 500) {
            nivel = "Iniciante Verde";
            pisoNivel = 0;
            tetoNivel = 500;
        } else if (pontuacaoTotal < 1500) {
            nivel = "Agente Sustentável";
            pisoNivel = 500;
            tetoNivel = 1500;
        } else if (pontuacaoTotal < 4000) {
            nivel = "Guardião EcoFlux";
            pisoNivel = 1500;
            tetoNivel = 4000;
        } else {
            nivel = "Embaixador Circular";
            pisoNivel = 4000;
            tetoNivel = 10000;
        }

        int progressoNivel = Math.min(100, (int) (((double) (pontuacaoTotal - pisoNivel) / (tetoNivel - pisoNivel)) * 100));

        return new EcoScoreResposta(
                usuario.getId(),
                usuario.getNome(),
                pontuacaoTotal,
                nivel,
                tetoNivel,
                progressoNivel,
                concluidas.size(),
                diversidadeMateriais,
                totalKg.setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcoScoreResposta> listarRankingEcoScore() {
        return usuarioRepositorio.findAllByStatusOrderByDataCriacaoDesc(StatusCadastro.ATIVO)
                .stream()
                .map(this::calcularEcoScoreUsuario)
                .sorted(Comparator.comparingInt(EcoScoreResposta::pontuacaoTotal).reversed())
                .limit(20)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcoSeloResposta> listarSelosUsuario(Usuario usuario) {
        EcoImpactoResposta impacto = calcularImpactoUsuario(usuario);
        EcoScoreResposta score = calcularEcoScoreUsuario(usuario);

        int coletas = (int) impacto.totalColetasConcluidas();
        int kgReciclados = impacto.totalResiduosRecicladosKg().setScale(0, RoundingMode.DOWN).intValue();
        int co2Evitado = impacto.co2EvitadoKg().setScale(0, RoundingMode.DOWN).intValue();
        int materiaisDistintos = score.diversidadeMateriais();

        return List.of(
                criarSelo("PRIMEIRO_PASSO", "Primeiro Passo Verde",
                        "Realizou sua primeira coleta sustentável na plataforma.",
                        "Concluir 1 coleta", "leaf", coletas, 1),

                criarSelo("HABITO_SUSTENTAVEL", "Hábito Sustentável",
                        "Manteve regularidade na destinação correta de resíduos.",
                        "Concluir 5 coletas", "recycle", coletas, 5),

                criarSelo("GUARDIAO_100KG", "Guardião 100kg",
                        "Desviou mais de 100 kg de resíduos dos aterros sanitários.",
                        "Reciclar 100 kg", "shield-check", kgReciclados, 100),

                criarSelo("MESTRE_TRIAGEM", "Mestre da Triagem",
                        "Separou e destinou corretamente 4 ou mais categorias de materiais.",
                        "Reciclar 4 tipos de materiais diferentes", "layers", materiaisDistintos, 4),

                criarSelo("NEUTRALIZADOR_CARBONO", "Neutralizador de Carbono",
                        "Evitou a emissão de 250 kg de CO2 equivalente na atmosfera.",
                        "Evitar 250 kg de CO2", "cloud-sun", co2Evitado, 250),

                criarSelo("EMBAIXADOR_CIRCULAR", "Embaixador da Economia Circular",
                        "Atingiu o patamar máximo de impacto e engajamento ambiental.",
                        "Alcançar 4.000 pontos no EcoScore", "award", score.pontuacaoTotal(), 4000)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EcoSeloResposta> listarCatalogoSelos() {
        return List.of(
                criarSelo("PRIMEIRO_PASSO", "Primeiro Passo Verde", "Realizou sua primeira coleta sustentável na plataforma.", "Concluir 1 coleta", "leaf", 0, 1),
                criarSelo("HABITO_SUSTENTAVEL", "Hábito Sustentável", "Manteve regularidade na destinação correta de resíduos.", "Concluir 5 coletas", "recycle", 0, 5),
                criarSelo("GUARDIAO_100KG", "Guardião 100kg", "Desviou mais de 100 kg de resíduos dos aterros sanitários.", "Reciclar 100 kg", "shield-check", 0, 100),
                criarSelo("MESTRE_TRIAGEM", "Mestre da Triagem", "Separou e destinou corretamente 4 ou mais categorias de materiais.", "Reciclar 4 tipos de materiais diferentes", "layers", 0, 4),
                criarSelo("NEUTRALIZADOR_CARBONO", "Neutralizador de Carbono", "Evitou a emissão de 250 kg de CO2 equivalente na atmosfera.", "Evitar 250 kg de CO2", "cloud-sun", 0, 250),
                criarSelo("EMBAIXADOR_CIRCULAR", "Embaixador da Economia Circular", "Atingiu o patamar máximo de impacto e engajamento ambiental.", "Alcançar 4.000 pontos no EcoScore", "award", 0, 4000)
        );
    }

    private List<Coleta> filtrarColetasConcluidas(List<Coleta> coletas) {
        return coletas.stream()
                .filter(c -> c.getStatus() != null &&
                        (c.getStatus().name().equals("CONCLUIDA") || c.getStatus().name().equals("REALIZADA")))
                .toList();
    }

    private EcoImpactoResposta montarImpacto(List<Coleta> concluidas) {
        Map<TipoMaterial, List<Coleta>> porMaterial = concluidas.stream()
                .collect(Collectors.groupingBy(Coleta::getTipoMaterial));

        List<ImpactoPorMaterialResposta> detalhes = new ArrayList<>();
        BigDecimal pesoTotalGeral = BigDecimal.ZERO;
        BigDecimal co2TotalGeral = BigDecimal.ZERO;

        for (TipoMaterial material : TipoMaterial.values()) {
            List<Coleta> lista = porMaterial.getOrDefault(material, List.of());
            if (lista.isEmpty()) continue;

            BigDecimal pesoMaterial = lista.stream()
                    .map(Coleta::getQuantidadeEstimadaKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal co2Material = pesoMaterial
                    .multiply(BigDecimal.valueOf(material.getFatorCarbonoEvitadoKg()))
                    .setScale(2, RoundingMode.HALF_UP);

            pesoTotalGeral = pesoTotalGeral.add(pesoMaterial);
            co2TotalGeral = co2TotalGeral.add(co2Material);

            detalhes.add(new ImpactoPorMaterialResposta(
                    material.name(),
                    material.getNomeExibicao(),
                    lista.size(),
                    pesoMaterial.setScale(2, RoundingMode.HALF_UP),
                    co2Material
            ));
        }

        BigDecimal arvores = co2TotalGeral.divide(KG_CO2_POR_ARVORE_ANO, 2, RoundingMode.HALF_UP);
        BigDecimal energia = pesoTotalGeral.multiply(KWH_ECONOMIZADO_POR_KG).setScale(2, RoundingMode.HALF_UP);
        BigDecimal agua = pesoTotalGeral.multiply(LITROS_AGUA_POR_KG).setScale(2, RoundingMode.HALF_UP);

        return new EcoImpactoResposta(
                concluidas.size(),
                pesoTotalGeral.setScale(2, RoundingMode.HALF_UP),
                co2TotalGeral.setScale(2, RoundingMode.HALF_UP),
                arvores,
                energia,
                agua,
                detalhes
        );
    }

    private EcoSeloResposta criarSelo(String codigo, String titulo, String descricao, String criterio,
                                      String icone, int atual, int meta) {
        boolean conquistado = atual >= meta;
        int percentual = Math.min(100, (int) (((double) atual / meta) * 100));
        return new EcoSeloResposta(codigo, titulo, descricao, criterio, icone, conquistado, atual, meta, percentual);
    }
}