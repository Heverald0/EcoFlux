package com.example.EcoFlux.dto;

import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class AmbientalDtos {
    private AmbientalDtos() {}

    public record FatorCadastro(@NotNull TipoMaterial material, @NotNull UnidadeMaterial unidade,
            @NotBlank @Size(max = 50) String versao, @NotBlank @Size(max = 1000) String fonte,
            @NotNull LocalDate vigencia,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 6) BigDecimal co2Kg,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 6) BigDecimal aguaL,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 6) BigDecimal energiaKwh,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 6) BigDecimal arvores) {}

    public record FatorResposta(Long id, TipoMaterial material, UnidadeMaterial unidade, String versao,
            String fonte, LocalDate vigencia, BigDecimal co2Kg, BigDecimal aguaL,
            BigDecimal energiaKwh, BigDecimal arvores) {
        public static FatorResposta de(FatorImpacto f) {
            return new FatorResposta(f.getId(), f.getMaterial(), f.getUnidade(), f.getVersao(), f.getFonte(),
                    f.getVigencia(), f.getCo2Kg(), f.getAguaL(), f.getEnergiaKwh(), f.getArvores());
        }
    }

    public record MetaCadastro(@NotNull Long instituicaoId, @NotBlank @Size(max = 150) String titulo,
            @NotNull IndicadorAmbiental indicador, @NotNull @DecimalMin("0.000001") @Digits(integer = 12, fraction = 6) BigDecimal alvo,
            @NotNull LocalDate inicio, @NotNull LocalDate fim) {}

    public record Impacto(BigDecimal co2Kg, BigDecimal aguaL, BigDecimal energiaKwh, BigDecimal arvores) {
        public static Impacto zero() {
            return new Impacto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        public Impacto somar(Impacto outro) {
            return new Impacto(co2Kg.add(outro.co2Kg), aguaL.add(outro.aguaL),
                    energiaKwh.add(outro.energiaKwh), arvores.add(outro.arvores));
        }
        public BigDecimal valor(IndicadorAmbiental indicador) {
            return switch (indicador) {
                case CO2_KG -> co2Kg;
                case AGUA_L -> aguaL;
                case ENERGIA_KWH -> energiaKwh;
                case ARVORES -> arvores;
            };
        }
    }

    public record MaterialTotal(TipoMaterial material, UnidadeMaterial unidade, BigDecimal quantidade,
                                BigDecimal percentual, Impacto impacto) {}
    public record Mes(String mes, Map<UnidadeMaterial, BigDecimal> quantidades, Impacto impacto) {}
    public record Resumo(LocalDate inicio, LocalDate fim, Map<UnidadeMaterial, BigDecimal> quantidades,
            long registrosConcluidos, Impacto impacto, long registrosSemFator,
            List<FatorResposta> fatoresUtilizados, List<MaterialTotal> materiais, List<Mes> evolucao) {}
    public record MetaResposta(Long id, String titulo, IndicadorAmbiental indicador, BigDecimal alvo,
            BigDecimal atual, BigDecimal percentual, LocalDate inicio, LocalDate fim, long registrosSemFator) {}
    public record Pontos(long frequencia, long separacao, long quantidade, long regularidade, long total) {}
    public record Score(Long instituicaoId, Pontos pontos, NivelSelo nivel, long proximoNivel,
                        String regraVersao, LocalDate inicio, LocalDate fim) {}
    public record Movimento(Long id, Long materialId, Pontos pontos, String regraVersao, LocalDate data) {
        public static Movimento de(MovimentoScore m) {
            return new Movimento(m.getId(), m.getMaterial().getId(), new Pontos(m.getFrequencia(),
                    m.getSeparacao(), m.getQuantidade(), m.getRegularidade(), m.getTotal()), m.getRegraVersao(), m.getDataReferencia());
        }
    }
    public record Ranking(Long instituicaoId, String nome, long pontos, NivelSelo nivel) {}
    public record Certificado(Long id, Long instituicaoId, String instituicao, NivelSelo nivel, long scoreBase,
            String codigo, String regraVersao, LocalDate emissao, LocalDate validade, String status,
            String motivo, LocalDateTime criadoEm) {
        public static Certificado de(CertificadoAmbiental c) {
            return new Certificado(c.getId(), c.getInstituicao().getId(), c.getInstituicao().getNome(), c.getNivel(),
                    c.getScoreBase(), c.getCodigo(), c.getRegraVersao(), c.getEmissao(), c.getValidade(),
                    c.isSubstituido() ? "SUPERADO" : c.getValidade().isBefore(LocalDate.now()) ? "EXPIRADO" : "VALIDO", c.getMotivo(), c.getDataCriacao());
        }
    }
}
