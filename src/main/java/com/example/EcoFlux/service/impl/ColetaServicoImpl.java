package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.AtualizarStatusColetaRequisicao;
import com.example.EcoFlux.dto.request.ColetaAgendamentoRequisicao;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.dto.request.response.EventoTimelineColetaResposta;
import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.HistoricoStatusColeta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.ColetaRepositorio;
import com.example.EcoFlux.repository.HistoricoStatusColetaRepositorio;
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
    private final HistoricoStatusColetaRepositorio historicoRepositorio;

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

        Instituicao cooperativa = validarCooperativa(requisicao.cooperativaId());

        Coleta novaColeta = Coleta.builder()
                .dataHoraAgendamento(requisicao.dataHoraAgendamento())
                .janelaHorario(requisicao.janelaHorario() != null && !requisicao.janelaHorario().isBlank()
                        ? requisicao.janelaHorario().trim()
                        : "COMERCIAL (08:00 - 18:00)")
                .tipoMaterial(requisicao.tipoMaterial())
                .quantidadeEstimadaKg(requisicao.quantidadeEstimadaKg())
                .observacoes(requisicao.observacoes() != null ? requisicao.observacoes().trim() : null)
                .logradouro(requisicao.logradouro().trim())
                .numero(requisicao.numero().trim())
                .bairro(requisicao.bairro().trim())
                .cidade(requisicao.cidade().trim())
                .estado(requisicao.estado().trim().toUpperCase())
                .cep(requisicao.cep().trim())
                .status(StatusColeta.ENVIADA) // COL-03: Estado inicial ENVIADA
                .usuarioSolicitante(usuarioSolicitante)
                .instituicao(instituicao)
                .cooperativa(cooperativa)
                .build();

        Coleta coletaSalva = coletaRepositorio.save(novaColeta);

        registrarEventoTimeline(coletaSalva, null, StatusColeta.ENVIADA,
                "Solicitacao de coleta criada e enviada para analise.", usuarioSolicitante);

        return ColetaResposta.converter(coletaSalva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColetaResposta> listarMinhasColetas(Usuario usuarioSolicitante, StatusColeta status) {
        List<Coleta> coletas = (status != null)
                ? coletaRepositorio.findAllByUsuarioSolicitanteAndStatusOrderByDataHoraAgendamentoDesc(usuarioSolicitante, status)
                : coletaRepositorio.findAllByUsuarioSolicitanteOrderByDataHoraAgendamentoDesc(usuarioSolicitante);

        return coletas.stream()
                .map(ColetaResposta::converter)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ColetaResposta> listarPorStatusOperacional(StatusColeta status) {
        List<Coleta> coletas = (status != null)
                ? coletaRepositorio.findAllByStatusOrderByDataHoraAgendamentoAsc(status)
                : coletaRepositorio.findAll();

        return coletas.stream()
                .map(ColetaResposta::converter)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ColetaResposta buscarPorId(Long id, Usuario usuarioAutenticado) {
        Coleta coleta = buscarEValidarAcessoLeitura(id, usuarioAutenticado);
        return ColetaResposta.converter(coleta);
    }

    @Override
    @Transactional
    public ColetaResposta atualizarStatus(Long id, AtualizarStatusColetaRequisicao requisicao, Usuario usuarioAutenticado) {
        Coleta coleta = coletaRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Coleta com ID %d nao encontrada.", id)));

        StatusColeta statusAtual = coleta.getStatus();
        StatusColeta novoStatus = requisicao.novoStatus();

        if (!statusAtual.podeTransicionarPara(novoStatus)) {
            throw new ExcecaoConflitoNegocio(String.format(
                    "Transicao de status invalida: nao e permitido mudar de '%s' para '%s'.",
                    statusAtual, novoStatus
            ));
        }

        if (requisicao.cooperativaId() != null) {
            Instituicao cooperativa = validarCooperativa(requisicao.cooperativaId());
            coleta.setCooperativa(cooperativa);
        }

        coleta.setStatus(novoStatus);
        Coleta coletaAtualizada = coletaRepositorio.save(coleta);

        String observacaoEvento = (requisicao.observacao() != null && !requisicao.observacao().isBlank())
                ? requisicao.observacao().trim()
                : String.format("Status atualizado de %s para %s.", statusAtual, novoStatus);

        registrarEventoTimeline(coletaAtualizada, statusAtual, novoStatus, observacaoEvento, usuarioAutenticado);

        return ColetaResposta.converter(coletaAtualizada);
    }

    @Override
    @Transactional
    public ColetaResposta cancelarColeta(Long id, Usuario usuarioAutenticado) {
        Coleta coleta = buscarEValidarAcessoLeitura(id, usuarioAutenticado);

        StatusColeta statusAtual = coleta.getStatus();
        if (!statusAtual.podeTransicionarPara(StatusColeta.CANCELADA)) {
            throw new ExcecaoConflitoNegocio(String.format(
                    "Nao e possivel cancelar uma coleta que se encontra no status '%s'.", statusAtual
            ));
        }

        coleta.setStatus(StatusColeta.CANCELADA);
        Coleta coletaCancelada = coletaRepositorio.save(coleta);

        registrarEventoTimeline(coletaCancelada, statusAtual, StatusColeta.CANCELADA,
                "Coleta cancelada pelo usuario.", usuarioAutenticado);

        return ColetaResposta.converter(coletaCancelada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoTimelineColetaResposta> consultarTimeline(Long coletaId, Usuario usuarioAutenticado) {
        buscarEValidarAcessoLeitura(coletaId, usuarioAutenticado);
        return historicoRepositorio.findAllByColetaIdOrderByDataRegistroAsc(coletaId)
                .stream()
                .map(EventoTimelineColetaResposta::converter)
                .toList();
    }

    private Coleta buscarEValidarAcessoLeitura(Long id, Usuario usuarioAutenticado) {
        Coleta coleta = coletaRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Coleta com ID %d nao encontrada.", id)));

        boolean ehSolicitante = coleta.getUsuarioSolicitante().getId().equals(usuarioAutenticado.getId());
        boolean ehResponsavelCooperativa = coleta.getCooperativa() != null
                && coleta.getCooperativa().getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId());
        boolean ehAdmin = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehSolicitante && !ehResponsavelCooperativa && !ehAdmin) {
            throw new ExcecaoConflitoNegocio("Acesso negado: voce nao tem permissao para acessar esta coleta.");
        }
        return coleta;
    }

    private Instituicao validarCooperativa(Long cooperativaId) {
        if (cooperativaId == null) {
            return null;
        }
        Instituicao cooperativa = instituicaoRepositorio.findById(cooperativaId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                        String.format("Cooperativa com ID %d nao encontrada.", cooperativaId)
                ));

        if (cooperativa.getTipo() != TipoInstituicao.COOPERATIVA) {
            throw new ExcecaoConflitoNegocio("A instituicao informada como coletora deve ser do tipo COOPERATIVA.");
        }
        return cooperativa;
    }

    private void registrarEventoTimeline(Coleta coleta, StatusColeta anterior, StatusColeta novo,
                                        String observacao, Usuario autor) {
        HistoricoStatusColeta evento = HistoricoStatusColeta.builder()
                .coleta(coleta)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .observacao(observacao)
                .usuarioAutor(autor)
                .build();
        historicoRepositorio.save(evento);
    }
}