package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusColeta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RotaMotoristaItemResposta(
        Long coletaId,
        LocalDateTime dataHoraAgendamento,
        String janelaHorario,
        StatusColeta status,
        String codigoMaterial,
        String nomeMaterial,
        BigDecimal quantidadeEstimadaKg,
        String observacoes,
        String enderecoCompleto,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        String localOrigemNome,
        String nomeResponsavelLocal,
        String telefoneResponsavelLocal
) {
    public static RotaMotoristaItemResposta converter(Coleta coleta) {
        Usuario responsavel = (coleta.getInstituicao() != null)
                ? coleta.getInstituicao().getUsuarioResponsavel()
                : coleta.getUsuarioSolicitante();

        String origem = (coleta.getInstituicao() != null)
                ? coleta.getInstituicao().getNome()
                : "Residência / Coleta Avulsa";

        String enderecoFormatado = String.format("%s, %s - %s, %s/%s (CEP: %s)",
                coleta.getLogradouro(),
                coleta.getNumero(),
                coleta.getBairro(),
                coleta.getCidade(),
                coleta.getEstado(),
                coleta.getCep());

        return new RotaMotoristaItemResposta(
                coleta.getId(),
                coleta.getDataHoraAgendamento(),
                coleta.getJanelaHorario(),
                coleta.getStatus(),
                coleta.getTipoMaterial().name(),
                coleta.getTipoMaterial().getNomeExibicao(),
                coleta.getQuantidadeEstimadaKg(),
                coleta.getObservacoes(),
                enderecoFormatado,
                coleta.getLogradouro(),
                coleta.getNumero(),
                coleta.getBairro(),
                coleta.getCidade(),
                coleta.getEstado(),
                coleta.getCep(),
                origem,
                responsavel.getNome(),
                responsavel.getTelefone()
        );
    }
}