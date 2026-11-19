package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.*;
import com.example.EcoFlux.dto.request.response.*;
import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.exception.*;
import com.example.EcoFlux.repository.*;
import com.example.EcoFlux.service.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ColetaServicoImpl implements ColetaServico {

    private final ColetaRepositorio coletaRepositorio;
    private final InstituicaoRepositorio instituicaoRepositorio;
    private final HistoricoStatusColetaRepositorio historicoRepositorio;
    private final PontoColetaRepositorio pontoRepositorio;
    private final EventoServico eventos;

    @Override
    @Transactional
    public ColetaResposta agendar(ColetaAgendamentoRequisicao requisicao, Usuario usuario) {
        Coleta coleta = new Coleta();
        coleta.setUsuarioSolicitante(usuario);
        preencher(coleta, requisicao, usuario);
        coleta.setStatus(StatusColeta.ENVIADA);
        coletaRepositorio.saveAndFlush(coleta);
        registrarEventoTimeline(coleta, null, StatusColeta.ENVIADA, "Solicitacao de coleta enviada para analise.", usuario);
        return ColetaResposta.converter(coleta);
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

    private Instituicao validarCooperativa(Long id) {
        if (id == null) return null;
        Instituicao cooperativa = instituicaoRepositorio.findById(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Cooperativa nao encontrada."));
        if (cooperativa.getTipo() != TipoInstituicao.COOPERATIVA) throw new ExcecaoRegraNegocio("A instituicao coletora deve ser uma cooperativa.");
        validarAtiva(cooperativa);
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

    @Override
    @Transactional
    public ColetaResposta editar(Long id, ColetaAgendamentoRequisicao requisicao, Usuario usuario) {
        Coleta coleta = obterBloqueada(id);
        if (!administrador(usuario) && !coleta.getUsuarioSolicitante().getId().equals(usuario.getId()))
            throw new AccessDeniedException("Apenas o solicitante pode editar a coleta.");
        if (coleta.getStatus() != StatusColeta.ENVIADA && coleta.getStatus() != StatusColeta.EM_ANALISE)
            throw new ExcecaoConflitoNegocio("A coleta so pode ser editada antes do agendamento.");
        preencher(coleta, requisicao, usuario);
        eventos.registrar(UUID.randomUUID().toString(), "COLETA_EDITADA", "COLETA", id, usuario,
                coleta.getUsuarioSolicitante(), coleta.getInstituicao(), "Solicitacao de coleta atualizada.");
        return ColetaResposta.converter(coleta);
    }

    private void preencher(Coleta coleta, ColetaAgendamentoRequisicao r, Usuario usuario) {
        if (r.dataHoraAgendamento().isBefore(LocalDateTime.now().plusHours(1)))
            throw new ExcecaoConflitoNegocio("O agendamento deve ter pelo menos uma hora de antecedencia.");
        Instituicao instituicao = r.instituicaoId() == null ? null : instituicaoRepositorio.findById(r.instituicaoId())
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Instituicao nao encontrada."));
        if (instituicao != null) {
            if (!administrador(usuario) && !instituicao.getUsuarioResponsavel().getId().equals(usuario.getId()))
                throw new AccessDeniedException("Instituicao pertence a outro responsavel.");
            validarAtiva(instituicao);
        }
        UnidadeMaterial unidade = r.unidade() == null ? UnidadeMaterial.KG : r.unidade();
        if (unidade == UnidadeMaterial.L && r.tipoMaterial() != TipoMaterial.OLEO)
            throw new ExcecaoRegraNegocio("A unidade L so e aceita para oleo.");
        PontoColeta ponto = r.pontoColetaId() == null ? null : pontoRepositorio.findById(r.pontoColetaId())
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Ponto de coleta nao encontrado."));
        if (ponto != null && (ponto.getStatus() != StatusPontoColeta.ATIVO || !ponto.getMateriaisAceitos().contains(r.tipoMaterial())))
            throw new ExcecaoConflitoNegocio("Ponto indisponivel para o material informado.");
        coleta.setDataHoraAgendamento(r.dataHoraAgendamento());
        coleta.setJanelaHorario(r.janelaHorario() == null || r.janelaHorario().isBlank() ? "COMERCIAL (08:00 - 18:00)" : r.janelaHorario().trim());
        coleta.setTipoMaterial(r.tipoMaterial());
        coleta.setQuantidadeEstimadaKg(r.quantidadeEstimadaKg());
        coleta.setUnidade(unidade);
        coleta.setObservacoes(r.observacoes() == null ? null : r.observacoes().trim());
        coleta.setLogradouro(r.logradouro().trim());
        coleta.setNumero(r.numero().trim());
        coleta.setBairro(r.bairro().trim());
        coleta.setCidade(r.cidade().trim());
        coleta.setEstado(r.estado().trim().toUpperCase(Locale.ROOT));
        coleta.setCep(r.cep().replace("-", ""));
        coleta.setInstituicao(instituicao);
        coleta.setCooperativa(validarCooperativa(r.cooperativaId()));
        coleta.setPontoColeta(ponto);
    }

    private Coleta obterBloqueada(Long id) {
        return coletaRepositorio.buscarParaAtualizar(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Coleta nao encontrada."));
    }

    private boolean administrador(Usuario usuario) { return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR; }

    private void validarAtiva(Instituicao instituicao) {
        if (instituicao.getStatus() != StatusInstituicao.ATIVO)
            throw new ExcecaoConflitoNegocio("A instituicao deve estar aprovada e ativa.");
    }
}