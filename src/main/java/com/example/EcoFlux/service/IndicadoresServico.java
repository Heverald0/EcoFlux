package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.AmbientalDtos.*;
import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRegraNegocio;
import com.example.EcoFlux.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndicadoresServico {
    private final MaterialServico materiais;
    private final FatorImpactoRepositorio fatores;
    private final MetaAmbientalRepositorio metas;
    private final EscopoAmbiental escopo;
    private final EventoServico eventos;
    private final RegistroMaterialRepositorio registrosRepositorio;

    public List<ImpactoInstituicao> ranking(LocalDate inicio, LocalDate fim, Usuario usuario) {
        var grupos = materiais.concluidos(null, inicio, fim, usuario).stream().filter(m -> m.getInstituicao() != null)
                .collect(Collectors.groupingBy(m -> m.getInstituicao().getId()));
        return grupos.entrySet().stream().map(g -> new ImpactoInstituicao(g.getKey(), g.getValue().getFirst().getInstituicao().getNome(),
                        quantidades(g.getValue()), totalImpacto(g.getValue()), g.getValue().stream().filter(m -> m.getFatorImpacto() == null).count()))
                .sorted(Comparator.comparing((ImpactoInstituicao i) -> i.impacto().co2Kg()).reversed().thenComparing(ImpactoInstituicao::instituicaoId))
                .limit(100).toList();
    }

    @Transactional
    public Map<String, Long> reprocessar(Long instituicaoId, LocalDate inicio, LocalDate fim, Usuario usuario) {
        escopo.exigirAdministrador(usuario);
        var pendentes = materiais.concluidos(instituicaoId, inicio, fim, usuario).stream()
                .filter(m -> m.getFatorImpacto() == null).map(RegistroMaterial::getId).sorted().limit(1000).toList();
        long vinculados = 0;
        for (Long id : pendentes) {
            RegistroMaterial material = registrosRepositorio.buscarParaAtualizar(id).orElseThrow();
            if (material.getFatorImpacto() != null) continue;
            var fator = fatores.findFirstByMaterialAndUnidadeAndVigenciaLessThanEqualOrderByVigenciaDescIdDesc(
                    material.getTipoMaterial(), material.getUnidade(), material.getDataRegistro()).orElse(null);
            if (fator == null) continue;
            material.setFatorImpacto(fator);
            eventos.registrar("impacto:material:" + id, "IMPACTO_REPROCESSADO", "material", id, usuario, null,
                    material.getInstituicao(), "Fator " + fator.getId() + ", versao " + fator.getVersao() + " vinculado ao registro.");
            vinculados++;
        }
        return Map.of("registrosAnalisados", (long) pendentes.size(), "fatoresVinculados", vinculados);
    }

    public record ImpactoInstituicao(Long instituicaoId, String nome, Map<UnidadeMaterial, BigDecimal> quantidades,
                                    Impacto impacto, long registrosSemFator) {}

    public Resumo resumo(Long instituicaoId, LocalDate inicio, LocalDate fim, Usuario usuario) {
        List<RegistroMaterial> registros = materiais.concluidos(instituicaoId, inicio, fim, usuario);
        Impacto impactoTotal = totalImpacto(registros);
        List<MaterialTotal> porMaterial = new ArrayList<>();
        for (TipoMaterial tipo : TipoMaterial.values()) {
            for (UnidadeMaterial unidade : UnidadeMaterial.values()) {
                List<RegistroMaterial> grupo = registros.stream()
                        .filter(m -> m.getTipoMaterial() == tipo && m.getUnidade() == unidade).toList();
                if (!grupo.isEmpty()) {
                    Impacto impacto = totalImpacto(grupo);
                    porMaterial.add(new MaterialTotal(tipo, unidade, quantidade(grupo),
                            percentual(impacto.co2Kg(), impactoTotal.co2Kg()), impacto));
                }
            }
        }
        List<Mes> meses = new ArrayList<>();
        for (YearMonth mes = YearMonth.from(inicio); !mes.isAfter(YearMonth.from(fim)); mes = mes.plusMonths(1)) {
            YearMonth atual = mes;
            List<RegistroMaterial> grupo = registros.stream().filter(m -> YearMonth.from(m.getDataRegistro()).equals(atual)).toList();
            meses.add(new Mes(mes.toString(), quantidades(grupo), totalImpacto(grupo)));
        }
        List<FatorResposta> utilizados = registros.stream().map(RegistroMaterial::getFatorImpacto).filter(Objects::nonNull)
                .collect(Collectors.toMap(FatorImpacto::getId, f -> f, (a, b) -> a, TreeMap::new))
                .values().stream().map(FatorResposta::de).toList();
        return new Resumo(inicio, fim, quantidades(registros), registros.size(), impactoTotal,
                registros.stream().filter(m -> m.getFatorImpacto() == null).count(), utilizados, porMaterial, meses);
    }

    @Transactional
    public FatorResposta cadastrarFator(FatorCadastro r, Usuario usuario) {
        escopo.exigirAdministrador(usuario);
        if (!r.unidade().aceita(r.material())) throw new ExcecaoRegraNegocio("Unidade incompativel com o material.");
        if (fatores.existsByMaterialAndUnidadeAndVersao(r.material(), r.unidade(), r.versao().trim()))
            throw new ExcecaoConflitoNegocio("Esta versao do fator ja foi cadastrada.");
        FatorImpacto fator = FatorImpacto.builder().material(r.material()).unidade(r.unidade()).versao(r.versao().trim())
                .fonte(r.fonte().trim()).vigencia(r.vigencia()).co2Kg(r.co2Kg()).aguaL(r.aguaL()).energiaKwh(r.energiaKwh())
                .arvores(r.arvores()).autor(usuario).build();
        fatores.save(fator);
        eventos.registrar("fator:" + fator.getId(), "FATOR_IMPACTO_CRIADO", "fator_impacto", fator.getId(),
                usuario, null, null, "Fator " + fator.getMaterial() + ", versao " + fator.getVersao() + ".");
        return FatorResposta.de(fator);
    }

    public Page<FatorResposta> fatores(int pagina, int tamanho) {
        return fatores.findAll(PageRequest.of(pagina, Math.min(tamanho, 100), Sort.by(Sort.Direction.DESC, "vigencia", "id")))
                .map(FatorResposta::de);
    }

    @Transactional
    public MetaResposta cadastrarMeta(MetaCadastro r, Usuario usuario) {
        Instituicao instituicao = escopo.instituicao(r.instituicaoId(), usuario);
        escopo.periodo(r.inicio(), r.fim());
        MetaAmbiental meta = MetaAmbiental.builder().instituicao(instituicao).titulo(r.titulo().trim())
                .indicador(r.indicador()).alvo(r.alvo()).inicio(r.inicio()).fim(r.fim()).autor(usuario).build();
        metas.save(meta);
        eventos.registrar("meta:" + meta.getId(), "META_AMBIENTAL_CRIADA", "meta_ambiental", meta.getId(),
                usuario, null, instituicao, "Meta ambiental cadastrada: " + meta.getTitulo() + ".");
        return meta(meta, usuario);
    }

    public List<MetaResposta> metas(Long instituicaoId, Usuario usuario) {
        escopo.instituicao(instituicaoId, usuario);
        return metas.findByInstituicaoIdOrderByFimAsc(instituicaoId).stream().map(m -> meta(m, usuario)).toList();
    }

    private MetaResposta meta(MetaAmbiental meta, Usuario usuario) {
        Resumo resumo = resumo(meta.getInstituicao().getId(), meta.getInicio(), meta.getFim(), usuario);
        BigDecimal atual = resumo.impacto().valor(meta.getIndicador());
        return new MetaResposta(meta.getId(), meta.getTitulo(), meta.getIndicador(), meta.getAlvo(), atual,
                percentual(atual, meta.getAlvo()), meta.getInicio(), meta.getFim(), resumo.registrosSemFator());
    }

    private Impacto totalImpacto(List<RegistroMaterial> registros) {
        return registros.stream().map(this::impacto).reduce(Impacto.zero(), Impacto::somar);
    }

    private Impacto impacto(RegistroMaterial material) {
        FatorImpacto fator = material.getFatorImpacto();
        if (fator == null) return Impacto.zero();
        BigDecimal quantidade = material.getQuantidade();
        return new Impacto(quantidade.multiply(fator.getCo2Kg()), quantidade.multiply(fator.getAguaL()),
                quantidade.multiply(fator.getEnergiaKwh()), quantidade.multiply(fator.getArvores()));
    }

    private Map<UnidadeMaterial, BigDecimal> quantidades(List<RegistroMaterial> registros) {
        Map<UnidadeMaterial, BigDecimal> totais = new EnumMap<>(UnidadeMaterial.class);
        for (UnidadeMaterial unidade : UnidadeMaterial.values())
            totais.put(unidade, quantidade(registros.stream().filter(m -> m.getUnidade() == unidade).toList()));
        return totais;
    }

    private BigDecimal quantidade(List<RegistroMaterial> registros) {
        return registros.stream().map(RegistroMaterial::getQuantidade).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO : parte.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }
}
