package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.AlterarStatusPontoRequisicao;
import com.example.EcoFlux.dto.request.PontoColetaRequisicao;
import com.example.EcoFlux.dto.request.response.PontoColetaResposta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.PontoColeta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusInstituicao;
import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.repository.PontoColetaRepositorio;
import com.example.EcoFlux.service.PontoColetaServico;
import com.example.EcoFlux.service.EventoServico;
import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PontoColetaServicoImpl implements PontoColetaServico {

    private final PontoColetaRepositorio pontoColetaRepositorio;
    private final InstituicaoRepositorio instituicaoRepositorio;
    private final EventoServico eventos;

    @Override
    @Transactional
    public PontoColetaResposta cadastrar(PontoColetaRequisicao requisicao, Usuario usuarioAutenticado) {
        String nomeNormalizado = requisicao.nome().trim();
        String cidadeNormalizada = requisicao.cidade().trim();

        if (pontoColetaRepositorio.existsByNomeIgnoreCaseAndCidadeIgnoreCase(nomeNormalizado, cidadeNormalizada)) {
            throw new ExcecaoConflitoNegocio(String.format(
                    "Ja existe um ponto de coleta cadastrado com o nome '%s' na cidade de %s.",
                    nomeNormalizado, cidadeNormalizada
            ));
        }

        Instituicao instituicao = validarInstituicaoVinculada(requisicao.instituicaoId(), usuarioAutenticado);

        PontoColeta novoPonto = PontoColeta.builder()
                .nome(nomeNormalizado)
                .nomeResponsavel(requisicao.nomeResponsavel().trim())
                .telefoneContato(requisicao.telefoneContato() != null ? requisicao.telefoneContato().trim() : null)
                .horarioFuncionamento(requisicao.horarioFuncionamento().trim())
                .logradouro(requisicao.logradouro().trim())
                .numero(requisicao.numero().trim())
                .bairro(requisicao.bairro().trim())
                .cidade(cidadeNormalizada)
                .estado(requisicao.estado().trim().toUpperCase())
                .cep(requisicao.cep().trim())
                .latitude(requisicao.latitude())
                .longitude(requisicao.longitude())
                .status(StatusPontoColeta.ATIVO)
                .materiaisAceitos(new HashSet<>(requisicao.materiaisAceitos()))
                .usuarioCadastro(usuarioAutenticado)
                .instituicao(instituicao)
                .build();

        PontoColeta salvo = pontoColetaRepositorio.save(novoPonto);
        registrar(salvo, usuarioAutenticado, "PONTO_CADASTRADO", "Ponto de coleta cadastrado.");
        return PontoColetaResposta.converter(salvo);
    }

    @Override
    @Transactional
    public PontoColetaResposta atualizar(Long id, PontoColetaRequisicao requisicao, Usuario usuarioAutenticado) {
        PontoColeta ponto = buscarEntidadeEValidarPermissao(id, usuarioAutenticado);

        if (pontoColetaRepositorio.existsByNomeIgnoreCaseAndCidadeIgnoreCaseAndIdNot(requisicao.nome().trim(), requisicao.cidade().trim(), id)) {
            throw new ExcecaoConflitoNegocio("Ja existe um ponto com este nome nesta cidade.");
        }

        Instituicao instituicao = validarInstituicaoVinculada(requisicao.instituicaoId(), usuarioAutenticado);

        ponto.setNome(requisicao.nome().trim());
        ponto.setNomeResponsavel(requisicao.nomeResponsavel().trim());
        ponto.setTelefoneContato(requisicao.telefoneContato() != null ? requisicao.telefoneContato().trim() : null);
        ponto.setHorarioFuncionamento(requisicao.horarioFuncionamento().trim());
        ponto.setLogradouro(requisicao.logradouro().trim());
        ponto.setNumero(requisicao.numero().trim());
        ponto.setBairro(requisicao.bairro().trim());
        ponto.setCidade(requisicao.cidade().trim());
        ponto.setEstado(requisicao.estado().trim().toUpperCase());
        ponto.setCep(requisicao.cep().trim());
        ponto.setLatitude(requisicao.latitude());
        ponto.setLongitude(requisicao.longitude());
        ponto.setMateriaisAceitos(new HashSet<>(requisicao.materiaisAceitos()));
        ponto.setInstituicao(instituicao);

        PontoColeta salvo = pontoColetaRepositorio.save(ponto);
        registrar(salvo, usuarioAutenticado, "PONTO_ATUALIZADO", "Dados do ponto de coleta atualizados.");
        return PontoColetaResposta.converter(salvo);
    }

    @Override
    @Transactional
    public PontoColetaResposta alterarStatus(Long id, AlterarStatusPontoRequisicao requisicao, Usuario usuarioAutenticado) {
        PontoColeta ponto = buscarEntidadeEValidarPermissao(id, usuarioAutenticado);

        if (requisicao.status() == StatusPontoColeta.MANUTENCAO
                && (requisicao.motivo() == null || requisicao.motivo().isBlank())) {
            throw new ExcecaoConflitoNegocio("Informe o motivo ao colocar o ponto de coleta em MANUTENCAO.");
        }

        if (ponto.getStatus() == requisicao.status()) return PontoColetaResposta.converter(ponto);
        ponto.setStatus(requisicao.status());
        ponto.setMotivoStatus(requisicao.status() == StatusPontoColeta.MANUTENCAO
                ? requisicao.motivo().trim()
                : null);

        PontoColeta salvo = pontoColetaRepositorio.save(ponto);
        registrar(salvo, usuarioAutenticado, "PONTO_STATUS_ALTERADO", "Status: " + ponto.getStatus()
                + (ponto.getMotivoStatus() == null ? "" : ". " + ponto.getMotivoStatus()));
        return PontoColetaResposta.converter(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public PontoColetaResposta buscarPorId(Long id) {
        PontoColeta ponto = pontoColetaRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                        String.format("Ponto de coleta com ID %d nao encontrado.", id)
                ));
        return PontoColetaResposta.converter(ponto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PontoColetaResposta> listarComFiltros(StatusPontoColeta status, String bairro, TipoMaterial material) {
        String bairroFiltro = (bairro != null && !bairro.isBlank()) ? bairro.trim() : null;
        return pontoColetaRepositorio.buscarComFiltros(status, bairroFiltro, material)
                .stream()
                .map(PontoColetaResposta::converter)
                .toList();
    }

    private PontoColeta buscarEntidadeEValidarPermissao(Long id, Usuario usuarioAutenticado) {
        PontoColeta ponto = pontoColetaRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                        String.format("Ponto de coleta com ID %d nao encontrado.", id)
                ));

        boolean ehCriador = ponto.getUsuarioCadastro().getId().equals(usuarioAutenticado.getId());
        boolean ehResponsavelInstituicao = ponto.getInstituicao() != null
                && ponto.getInstituicao().getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId());
        boolean ehAdmin = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehCriador && !ehResponsavelInstituicao && !ehAdmin) {
            throw new AccessDeniedException("Voce nao possui permissao para alterar este ponto de coleta.");
        }
        return ponto;
    }

    private Instituicao validarInstituicaoVinculada(Long instituicaoId, Usuario usuarioAutenticado) {
        if (instituicaoId == null) {
            return null;
        }
        Instituicao instituicao = instituicaoRepositorio.findById(instituicaoId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                        String.format("Instituicao com ID %d nao encontrada.", instituicaoId)
                ));

        if (!instituicao.getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId())
                && usuarioAutenticado.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            throw new AccessDeniedException("Apenas o responsavel pela instituicao pode vincular pontos de coleta a ela.");
        }
        if (instituicao.getStatus() != StatusInstituicao.ATIVO) {
            throw new ExcecaoConflitoNegocio("A instituicao precisa estar ativa para cadastrar pontos de coleta.");
        }
        return instituicao;
    }

    private void registrar(PontoColeta ponto, Usuario autor, String tipo, String mensagem) {
        eventos.registrar(UUID.randomUUID().toString(), tipo, "PONTO_COLETA", ponto.getId(), autor, null, ponto.getInstituicao(), mensagem);
    }
}
