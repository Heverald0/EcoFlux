package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.AtualizarStatusColetaRequisicao;
import com.example.EcoFlux.dto.request.ColetaAgendamentoRequisicao;
import com.example.EcoFlux.dto.request.ConcluirColetaMotoristaRequisicao;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.dto.request.response.EventoTimelineColetaResposta;
import com.example.EcoFlux.dto.request.response.RotaMotoristaItemResposta;
import com.example.EcoFlux.entity.Coleta;
import com.example.EcoFlux.entity.HistoricoStatusColeta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.MembroInstituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PapelMembroInstituicao;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusColeta;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.ColetaRepositorio;
import com.example.EcoFlux.repository.HistoricoStatusColetaRepositorio;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.repository.MembroInstituicaoRepositorio;
import com.example.EcoFlux.repository.UsuarioRepositorio;
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
    private final UsuarioRepositorio usuarioRepositorio;
    private final MembroInstituicaoRepositorio membroRepositorio;

    @Override
    @Transactional
    public ColetaResposta agendar(ColetaAgendamentoRequisicao requisicao, Usuario usuarioSolicitante) {
        if (requisicao.dataHoraAgendamento().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ExcecaoConflitoNegocio("O agendamento deve ser realizado com no minimo 1 hora de antecedencia.");
        }

        boolean ehAdmin = usuarioSolicitante.getPerfil() == PerfilUsuario.ADMINISTRADOR;
        boolean ehGestorInstituicao = usuarioSolicitante.getPerfil() == PerfilUsuario.INSTITUICAO;

        Instituicao instituicao = null;
        if (requisicao.instituicaoId() == null) {
            if (!ehGestorInstituicao && !ehAdmin) {
                throw new ExcecaoConflitoNegocio(
                        "Apenas instituicoes (condominios ou empresas), moradores vinculados ao seu condominio ou administradores podem agendar coletas."
                );
            }
        } else {
            instituicao = instituicaoRepositorio.findById(requisicao.instituicaoId())
                    .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                            String.format("Instituicao com ID %d nao encontrada.", requisicao.instituicaoId())
                    ));

            if (instituicao.getTipo() == TipoInstituicao.COOPERATIVA) {
                throw new ExcecaoConflitoNegocio("Cooperativas nao podem ser informadas como instituicao solicitante.");
            }

            boolean ehResponsavel = instituicao.getUsuarioResponsavel().getId().equals(usuarioSolicitante.getId());
            boolean ehMoradorAtivo = membroRepositorio.findByInstituicaoAndUsuario(instituicao, usuarioSolicitante)
                    .map(v -> Boolean.TRUE.equals(v.getAtivo()) && v.getPapel() == PapelMembroInstituicao.MORADOR)
                    .orElse(false);

            if (!ehResponsavel && !ehMoradorAtivo && !ehAdmin) {
                throw new ExcecaoConflitoNegocio(
                        "Apenas o responsavel ou moradores ativos vinculados a esta instituicao podem agendar coletas para ela."
                );
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
                .status(StatusColeta.ENVIADA)
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
        if (novoStatus == StatusColeta.CONCLUIDA || novoStatus == StatusColeta.REALIZADA) {
            coleta.setDataConclusao(LocalDateTime.now());
        }

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

    @Override
    @Transactional
    public ColetaResposta atribuirMotorista(Long coletaId, Long motoristaId, Usuario usuarioAutenticado) {
        Coleta coleta = coletaRepositorio.findById(coletaId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Coleta nao encontrada."));

        if (coleta.getCooperativa() == null) {
            throw new ExcecaoConflitoNegocio("A coleta precisa estar vinculada a uma cooperativa antes de atribuir um motorista.");
        }

        boolean ehAdminCooperativa = coleta.getCooperativa().getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId());
        boolean ehAdminGeral = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;
        if (!ehAdminCooperativa && !ehAdminGeral) {
            throw new ExcecaoConflitoNegocio("Apenas o administrador da cooperativa responsavel pode escalar motoristas.");
        }

        Usuario motorista = usuarioRepositorio.findById(motoristaId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Motorista nao encontrado."));

        MembroInstituicao vinculo = membroRepositorio.findByInstituicaoAndUsuario(coleta.getCooperativa(), motorista)
                .orElseThrow(() -> new ExcecaoConflitoNegocio("O usuario informado nao e membro desta cooperativa."));

        if (!Boolean.TRUE.equals(vinculo.getAtivo()) || vinculo.getPapel() != PapelMembroInstituicao.MOTORISTA) {
            throw new ExcecaoConflitoNegocio("O membro informado nao possui a funcao ativa de MOTORISTA nesta cooperativa.");
        }

        coleta.setMotorista(motorista);
        StatusColeta statusAnterior = coleta.getStatus();
        if (statusAnterior == StatusColeta.AGENDADA) {
            coleta.setStatus(StatusColeta.EM_ROTA);
        }

        Coleta salva = coletaRepositorio.save(coleta);

        registrarEventoTimeline(salva, statusAnterior, salva.getStatus(),
                String.format("Coleta atribuida ao motorista %s.", motorista.getNome()), usuarioAutenticado);

        return ColetaResposta.converter(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RotaMotoristaItemResposta> listarMinhaRotaMotorista(Usuario motoristaAutenticado) {
        if (motoristaAutenticado.getPerfil() != PerfilUsuario.MOTORISTA
                && motoristaAutenticado.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            throw new ExcecaoConflitoNegocio("Acesso restrito a motoristas cadastrados pelas cooperativas.");
        }

        List<StatusColeta> statusDaRota = List.of(StatusColeta.AGENDADA, StatusColeta.EM_ROTA, StatusColeta.EM_ANDAMENTO);
        return coletaRepositorio.findRotaAtivaDoMotorista(motoristaAutenticado.getId(), statusDaRota)
                .stream()
                .map(RotaMotoristaItemResposta::converter)
                .toList();
    }

    @Override
    @Transactional
    public ColetaResposta concluirEntregaMotorista(Long coletaId, ConcluirColetaMotoristaRequisicao requisicao, Usuario motoristaAutenticado) {
        Coleta coleta = coletaRepositorio.findById(coletaId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Coleta nao encontrada."));

        boolean ehMotoristaEscalado = coleta.getMotorista() != null
                && coleta.getMotorista().getId().equals(motoristaAutenticado.getId());
        boolean ehAdminGeral = motoristaAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehMotoristaEscalado && !ehAdminGeral) {
            throw new ExcecaoConflitoNegocio("Acesso negado: esta coleta nao esta atribuida a sua rota.");
        }

        StatusColeta statusAtual = coleta.getStatus();
        if (!statusAtual.podeTransicionarPara(StatusColeta.CONCLUIDA)) {
            throw new ExcecaoConflitoNegocio(String.format(
                    "Nao e possivel concluir a coleta no status atual '%s'.", statusAtual));
        }

        String ultimos4Digitados = requisicao.documentoRecebedor() != null
                ? requisicao.documentoRecebedor().replaceAll("\\D", "")
                : "";
        String cpfSolicitante = (coleta.getUsuarioSolicitante() != null
                && coleta.getUsuarioSolicitante().getDocumento() != null)
                ? coleta.getUsuarioSolicitante().getDocumento().replaceAll("\\D", "")
                : "";
        String cpfResponsavel = (coleta.getInstituicao() != null
                && coleta.getInstituicao().getUsuarioResponsavel() != null
                && coleta.getInstituicao().getUsuarioResponsavel().getDocumento() != null)
                ? coleta.getInstituicao().getUsuarioResponsavel().getDocumento().replaceAll("\\D", "")
                : "";

        boolean bateSolicitante = cpfSolicitante.length() >= 4 && cpfSolicitante.endsWith(ultimos4Digitados);
        boolean bateResponsavel = cpfResponsavel.length() >= 4 && cpfResponsavel.endsWith(ultimos4Digitados);

        if (ultimos4Digitados.length() != 4 || (!bateSolicitante && !bateResponsavel)) {
            throw new ExcecaoConflitoNegocio(
                    "Codigo incorreto: informe os 4 ultimos digitos do CPF de quem solicitou a coleta ou do responsavel pela instituicao."
            );
        }

        if (requisicao.pesoRealKg() != null) {
            coleta.setPesoRealKg(requisicao.pesoRealKg());
        }
        coleta.setNomeRecebedor(requisicao.nomeRecebedor().trim());
        coleta.setDocumentoRecebedor(ultimos4Digitados);
        coleta.setAssinaturaDigital(requisicao.assinaturaDigital().trim());
        coleta.setDataConclusao(LocalDateTime.now());
        coleta.setStatus(StatusColeta.CONCLUIDA);

        if (requisicao.observacoes() != null && !requisicao.observacoes().isBlank()) {
            coleta.setObservacoes(requisicao.observacoes().trim());
        }

        Coleta concluida = coletaRepositorio.save(coleta);

        registrarEventoTimeline(concluida, statusAtual, StatusColeta.CONCLUIDA,
                String.format("Coleta concluida pelo motorista %s. Recebido e assinado por: %s.",
                        motoristaAutenticado.getNome(), concluida.getNomeRecebedor()),
                motoristaAutenticado);

        return ColetaResposta.converter(concluida);
    }

    private Coleta buscarEValidarAcessoLeitura(Long id, Usuario usuarioAutenticado) {
        Coleta coleta = coletaRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Coleta com ID %d nao encontrada.", id)));

        boolean ehSolicitante = coleta.getUsuarioSolicitante().getId().equals(usuarioAutenticado.getId());
        boolean ehResponsavelInstituicao = coleta.getInstituicao() != null
                && coleta.getInstituicao().getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId());
        boolean ehMembroInstituicao = coleta.getInstituicao() != null
                && membroRepositorio.existsByInstituicaoIdAndUsuarioIdAndAtivoTrue(
                coleta.getInstituicao().getId(), usuarioAutenticado.getId());
        boolean ehResponsavelCooperativa = coleta.getCooperativa() != null
                && coleta.getCooperativa().getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId());
        boolean ehMotoristaEscalado = coleta.getMotorista() != null
                && coleta.getMotorista().getId().equals(usuarioAutenticado.getId());
        boolean ehAdmin = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehSolicitante && !ehResponsavelInstituicao && !ehMembroInstituicao
                && !ehResponsavelCooperativa && !ehMotoristaEscalado && !ehAdmin) {
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