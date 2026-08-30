package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.ColetaAgendamentoRequisicao;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.ColetaRepositorio;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.service.ColetaServico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ColetaServicoImpl implements ColetaServico {

    private final ColetaRepositorio coletaRepositorio;
    private final InstituicaoRepositorio instituicaoRepositorio;

    @Override
    @Transactional
    public ColetaResposta agendar(ColetaAgendamentoRequisicao requisicao, Usuario usuarioSolicitante) {
        if (requisicao.dataHoraAgendamento().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ExcecaoConflitoNegocio("O agendamento deve ser realizado com no minimo 1 hora de antecedencia.");
        }

        Instituicao instituicao = null;
        if (requisicao.instituicaoId() != null) {
            instituicao = instituicaoRepositorio.findById(requisicao.instituicaoId())
                    .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                            String.format("Instituicao com ID %d nao encontrada.", requisicao.instituicaoId())
                    ));

            if (!instituicao.getUsuarioResponsavel().getId().equals(usuarioSolicitante.getId()) 
                    && usuarioSolicitante.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
                throw new ExcecaoConflitoNegocio("Apenas o responsavel pela instituicao pode agendar coletas em seu nome.");
            }
        }

        Coleta novaColeta = Coleta.builder()
                .dataHoraAgendamento(requisicao.dataHoraAgendamento())
                .tipoMaterial(requisicao.tipoMaterial())
                .quantidadeEstimadaKg(requisicao.quantidadeEstimadaKg())
                .observacoes(requisicao.observacoes() != null ? requisicao.observacoes().trim() : null)
                .logradouro(requisicao.logradouro().trim())
                .numero(requisicao.numero().trim())
                .bairro(requisicao.bairro().trim())
                .cidade(requisicao.cidade().trim())
                .estado(requisicao.estado().trim().toUpperCase())
                .cep(requisicao.cep().trim())
                .status(StatusColeta.AGENDADA) // Status inicial padrão (RF04.02)
                .usuarioSolicitante(usuarioSolicitante)
                .instituicao(instituicao)
                .build();

        Coleta coletaSalva = coletaRepositorio.save(novaColeta);
        return ColetaResposta.converter(coletaSalva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColetaResposta> listarMinhasColetas(Usuario usuarioSolicitante) {
        return coletaRepositorio.findAllByUsuarioSolicitanteOrderByDataHoraAgendamentoDesc(usuarioSolicitante)
                .stream()
                .map(ColetaResposta::converter)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ColetaResposta buscarPorId(Long id, Usuario usuarioAutenticado) {
        Coleta coleta = coletaRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Coleta com ID %d nao encontrada.", id)));

        boolean ehSolicitante = coleta.getUsuarioSolicitante().getId().equals(usuarioAutenticado.getId());
        boolean ehAdmin = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehSolicitante && !ehAdmin) {
            throw new ExcecaoConflitoNegocio("Acesso negado: voce nao tem permissao para visualizar esta coleta.");
        }

        return ColetaResposta.converter(coleta);
    }

    @Override
    @Transactional
    public ColetaResposta cancelarColeta(Long id, Usuario usuarioAutenticado) {
        Coleta coleta = coletaRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Coleta com ID %d nao encontrada.", id)));

        boolean ehSolicitante = coleta.getUsuarioSolicitante().getId().equals(usuarioAutenticado.getId());
        boolean ehAdmin = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehSolicitante && !ehAdmin) {
            throw new ExcecaoConflitoNegocio("Acesso negado: voce nao tem permissao para cancelar esta coleta.");
        }

        if (coleta.getStatus() == StatusColeta.CONCLUIDA) {
            throw new ExcecaoConflitoNegocio("Nao e possivel cancelar uma coleta que ja foi concluida.");
        }

        if (coleta.getStatus() == StatusColeta.CANCELADA) {
            throw new ExcecaoConflitoNegocio("Esta coleta ja se encontra cancelada.");
        }

        coleta.setStatus(StatusColeta.CANCELADA);
        return ColetaResposta.converter(coletaRepositorio.save(coleta));
    }
}