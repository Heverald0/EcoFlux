package com.example.EcoFlux.dto;

import com.example.EcoFlux.entity.RegistroMaterial;
import com.example.EcoFlux.entity.enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MaterialDtos {
    private MaterialDtos() {}

    public record Cadastro(
            @NotNull TipoMaterial material,
            @NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal quantidade,
            @NotNull UnidadeMaterial unidade,
            @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal volume,
            @NotBlank @Size(max = 200) String origem,
            @NotBlank @Size(max = 200) String destino,
            @NotNull @PastOrPresent LocalDate data,
            Long instituicaoId,
            Long coletaId) {}

    public record AlterarStatus(@NotNull StatusMaterial status) {}

    public record Resposta(Long id, TipoMaterial material, BigDecimal quantidade,
                           UnidadeMaterial unidade, BigDecimal volume, String origem, String destino,
                           StatusMaterial status, LocalDate data, Long instituicaoId, Long coletaId,
                           LocalDateTime efetivadoEm) {
        public static Resposta de(RegistroMaterial m) {
            return new Resposta(m.getId(), m.getTipoMaterial(), m.getQuantidade(), m.getUnidade(),
                    m.getVolume(), m.getOrigem(), m.getDestino(), m.getStatus(), m.getDataRegistro(),
                    m.getInstituicao() == null ? null : m.getInstituicao().getId(),
                    m.getColeta() == null ? null : m.getColeta().getId(), m.getDataEfetivacao());
        }
    }
}
