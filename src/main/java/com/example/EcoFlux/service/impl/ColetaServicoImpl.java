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
@Transactional(readOnly = true)
public class ColetaServicoImpl implements ColetaServico {
    private final ColetaRepositorio coletaRepositorio;
    private final InstituicaoRepositorio instituicaoRepositorio;
    private final HistoricoStatusColetaRepositorio historicoRepositorio;
    private final PontoColetaRepositorio pontoRepositorio;
    private final EventoServico eventos;
    private final MaterialServico materiais;

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

    @Override
    public List<ColetaResposta> listarMinhasColetas(Usuario usuario, StatusColeta status) {
        return listar(usuario, status, null, null, false, PageRequest.of(0, 100)).getContent();
    }

    @Override
    public List<ColetaResposta> listarPorStatusOperacional(StatusColeta status, Usuario usuario) {
        return listar(usuario, status, null, null, true, PageRequest.of(0, 100)).getContent();
    }

    @Override
    public Page<ColetaResposta> listar(Usuario usuario, StatusColeta status, LocalDate inicio, LocalDate fim,
                                      boolean operacional, Pageable pagina) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) throw new ExcecaoRegraNegocio("Periodo invalido.");
        if (operacional && !operador(usuario) && usuario.getPerfil() != PerfilUsuario.COOPERATIVA)
            throw new AccessDeniedException("Fila operacional restrita a administradores e cooperativas.");
        Specification<Coleta> filtro = (root, query, cb) -> {
            List<Predicate> regras = new ArrayList<>();
            if (!operador(usuario) || !operacional) {
                regras.add(operacional ? cb.equal(root.get("cooperativa").get("usuarioResponsavel").get("id"), usuario.getId())
                        : cb.equal(root.get("usuarioSolicitante").get("id"), usuario.getId()));
            }
            if (status != null) regras.add(cb.equal(root.get("status"), status));
            if (inicio != null) regras.add(cb.greaterThanOrEqualTo(root.get("dataHoraAgendamento"), inicio.atStartOfDay()));
            if (fim != null) regras.add(cb.lessThan(root.get("dataHoraAgendamento"), fim.plusDays(1).atStartOfDay()));
            return cb.and(regras.toArray(Predicate[]::new));
        };
        return coletaRepositorio.findAll(filtro, PageRequest.of(pagina.getPageNumber(), Math.min(100, pagina.getPageSize()),
                Sort.by(Sort.Direction.DESC, "dataHoraAgendamento", "id"))).map(ColetaResposta::converter);
    }

    @Override
    public ColetaResposta buscarPorId(Long id, Usuario usuario) {
        return ColetaResposta.converter(buscarEValidarAcessoLeitura(id, usuario));
    }

    @Override
    @Transactional
    public ColetaResposta atualizarStatus(Long id, AtualizarStatusColetaRequisicao requisicao, Usuario usuario) {
        Coleta coleta = obterBloqueada(id);
        if (!operador(usuario) && !cooperativaResponsavel(coleta, usuario))
            throw new AccessDeniedException("Somente a cooperativa responsavel ou o administrador pode atualizar a coleta.");
        if (coleta.getCooperativa() != null) validarAtiva(coleta.getCooperativa());
        StatusColeta atual = coleta.getStatus();
        StatusColeta novo = requisicao.novoStatus();
        if (novo == null || !atual.podeTransicionarPara(novo))
            throw new ExcecaoConflitoNegocio("Transicao de status invalida: " + atual + " para " + novo + ".");
        if (requisicao.cooperativaId() != null) {
            if (!operador(usuario) && (coleta.getCooperativa() == null || !coleta.getCooperativa().getId().equals(requisicao.cooperativaId())))
                throw new AccessDeniedException("Somente o administrador pode transferir coletas entre cooperativas.");
            coleta.setCooperativa(validarCooperativa(requisicao.cooperativaId()));
        }
        if ((novo == StatusColeta.AGENDADA || novo == StatusColeta.EM_ROTA || novo == StatusColeta.EM_ANDAMENTO) && coleta.getCooperativa() == null)
            throw new ExcecaoConflitoNegocio("Vincule uma cooperativa antes de agendar a coleta.");
        if (novo == StatusColeta.REALIZADA || novo == StatusColeta.CONCLUIDA) {
            if (requisicao.quantidadeReal() == null || requisicao.quantidadeReal().signum() <= 0
                    || requisicao.quantidadeReal().scale() > 2 || requisicao.quantidadeReal().precision() > 12)
                throw new ExcecaoRegraNegocio("Informe a quantidade realmente coletada, positiva, com ate duas casas decimais.");
            coleta.setQuantidadeReal(requisicao.quantidadeReal());
            coleta.setDataConclusao(LocalDateTime.now());
        }
        coleta.setStatus(novo);
        registrarEventoTimeline(coleta, atual, novo, requisicao.observacao() == null || requisicao.observacao().isBlank()
                ? "Status atualizado de " + atual + " para " + novo + "." : requisicao.observacao().trim(), usuario);
        if (novo == StatusColeta.REALIZADA || novo == StatusColeta.CONCLUIDA) materiais.registrarColetaConcluida(coleta, usuario);
        return ColetaResposta.converter(coleta);
    }

    @Override
    @Transactional
    public ColetaResposta cancelarColeta(Long id, Usuario usuario) {
        Coleta coleta = obterBloqueada(id);
        validarLeitura(coleta, usuario);
        if (!coleta.getStatus().podeTransicionarPara(StatusColeta.CANCELADA))
            throw new ExcecaoConflitoNegocio("Coleta ja encerrada.");
        if (!operador(usuario) && !cooperativaResponsavel(coleta, usuario)
                && (coleta.getStatus() == StatusColeta.EM_ROTA || coleta.getStatus() == StatusColeta.EM_ANDAMENTO))
            throw new ExcecaoConflitoNegocio("A coleta em execucao deve ser cancelada pela cooperativa.");
        StatusColeta anterior = coleta.getStatus();
        coleta.setStatus(StatusColeta.CANCELADA);
        registrarEventoTimeline(coleta, anterior, StatusColeta.CANCELADA, "Coleta cancelada.", usuario);
        return ColetaResposta.converter(coleta);
    }

    @Override
    public List<EventoTimelineColetaResposta> consultarTimeline(Long id, Usuario usuario) {
        buscarEValidarAcessoLeitura(id, usuario);
        return historicoRepositorio.findAllByColetaIdOrderByDataRegistroAsc(id).stream().map(EventoTimelineColetaResposta::converter).toList();
    }

    private Coleta buscarEValidarAcessoLeitura(Long id, Usuario usuario) {
        Coleta coleta = coletaRepositorio.findById(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Coleta nao encontrada."));
        validarLeitura(coleta, usuario);
        return coleta;
    }

    private Coleta obterBloqueada(Long id) {
        return coletaRepositorio.buscarParaAtualizar(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Coleta nao encontrada."));
    }

    private void validarLeitura(Coleta coleta, Usuario usuario) {
        if (!operador(usuario) && !coleta.getUsuarioSolicitante().getId().equals(usuario.getId()) && !cooperativaResponsavel(coleta, usuario))
            throw new AccessDeniedException("Voce nao possui acesso a esta coleta.");
    }

    private boolean administrador(Usuario usuario) { return usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR; }
    private boolean operador(Usuario usuario) { return administrador(usuario) || usuario.getPerfil() == PerfilUsuario.OPERADOR; }

    private boolean cooperativaResponsavel(Coleta coleta, Usuario usuario) {
        return coleta.getCooperativa() != null && coleta.getCooperativa().getUsuarioResponsavel().getId().equals(usuario.getId())
                && usuario.getPerfil() == PerfilUsuario.COOPERATIVA;
    }

    private void validarAtiva(Instituicao instituicao) {
        if (instituicao.getStatus() != StatusInstituicao.ATIVO)
            throw new ExcecaoConflitoNegocio("A instituicao deve estar aprovada e ativa.");
    }

    private Instituicao validarCooperativa(Long id) {
        if (id == null) return null;
        Instituicao cooperativa = instituicaoRepositorio.findById(id).orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Cooperativa nao encontrada."));
        if (cooperativa.getTipo() != TipoInstituicao.COOPERATIVA) throw new ExcecaoRegraNegocio("A instituicao coletora deve ser uma cooperativa.");
        validarAtiva(cooperativa);
        return cooperativa;
    }

    private void registrarEventoTimeline(Coleta coleta, StatusColeta anterior, StatusColeta novo, String observacao, Usuario autor) {
        historicoRepositorio.save(HistoricoStatusColeta.builder().coleta(coleta).statusAnterior(anterior).statusNovo(novo)
                .observacao(observacao).usuarioAutor(autor).build());
        eventos.registrar("coleta:" + coleta.getId() + ":" + novo, "COLETA_" + novo, "COLETA", coleta.getId(), autor,
                coleta.getUsuarioSolicitante(), coleta.getInstituicao(), observacao);
    }
}
