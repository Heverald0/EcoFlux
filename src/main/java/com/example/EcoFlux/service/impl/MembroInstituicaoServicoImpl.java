package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.VincularMembroRequisicao;
import com.example.EcoFlux.dto.request.response.MembroInstituicaoResposta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.MembroInstituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PapelMembroInstituicao;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.entity.enums.TipoInstituicao;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.repository.MembroInstituicaoRepositorio;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.service.MembroInstituicaoServico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembroInstituicaoServicoImpl implements MembroInstituicaoServico {

    private final InstituicaoRepositorio instituicaoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final MembroInstituicaoRepositorio membroRepositorio;

    @Override
    @Transactional
    public MembroInstituicaoResposta vincularMembro(Long instituicaoId, VincularMembroRequisicao requisicao, Usuario usuarioAutenticado) {
        Instituicao instituicao = buscarEValidarAdminMaster(instituicaoId, usuarioAutenticado);

        if (instituicao.getStatus() != StatusCadastro.ATIVO) {
            throw new ExcecaoConflitoNegocio("A instituicao precisa estar ATIVA para vincular membros.");
        }

        if (requisicao.papel() == PapelMembroInstituicao.MOTORISTA && instituicao.getTipo() != TipoInstituicao.COOPERATIVA) {
            throw new ExcecaoConflitoNegocio("Somente instituicoes do tipo COOPERATIVA podem vincular usuarios com a funcao de MOTORISTA.");
        }

        String cpfLimpo = requisicao.cpf().replaceAll("[^0-9]", "").trim();
        String emailFormatado = requisicao.email().trim().toLowerCase();

        if (cpfLimpo.isBlank()) {
            throw new ExcecaoConflitoNegocio("CPF invalido. Revise os dados informados e tente novamente.");
        }

        Usuario usuarioPorCpf = usuarioRepositorio.findByDocumento(cpfLimpo)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                        "Nenhum usuario encontrado com o CPF informado. Revise os dados ou solicite que o usuario crie sua conta primeiro."));

        Usuario usuarioPorEmail = usuarioRepositorio.findByEmail(emailFormatado)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(
                        "Nenhum usuario encontrado com o e-mail informado. Revise os dados e tente novamente."));

        if (!usuarioPorCpf.getId().equals(usuarioPorEmail.getId())) {
            throw new ExcecaoConflitoNegocio(
                    "Divergencia de dados: o CPF e o e-mail informados nao pertencem ao mesmo usuario cadastrado. Revise os dados e insira novamente.");
        }

        if (usuarioPorCpf.getStatus() != StatusCadastro.ATIVO) {
            throw new ExcecaoConflitoNegocio("O usuario informado nao esta com o cadastro ATIVO na plataforma.");
        }

        if (instituicao.getUsuarioResponsavel().getId().equals(usuarioPorCpf.getId())) {
            throw new ExcecaoConflitoNegocio("Este usuario ja e o Administrador Master responsavel por esta instituicao.");
        }

        MembroInstituicao vinculo = membroRepositorio.findByInstituicaoAndUsuario(instituicao, usuarioPorCpf)
                .map(existente -> {
                    if (Boolean.TRUE.equals(existente.getAtivo())) {
                        throw new ExcecaoConflitoNegocio("Este usuario ja esta vinculado como membro ativo desta instituicao.");
                    }
                    existente.setAtivo(true);
                    existente.setPapel(requisicao.papel());
                    return existente;
                })
                .orElseGet(() -> MembroInstituicao.builder()
                        .instituicao(instituicao)
                        .usuario(usuarioPorCpf)
                        .papel(requisicao.papel())
                        .ativo(true)
                        .build());

        MembroInstituicao salvo = membroRepositorio.save(vinculo);

        if (requisicao.papel() == PapelMembroInstituicao.MOTORISTA && usuarioPorCpf.getPerfil() == PerfilUsuario.USUARIO) {
            usuarioPorCpf.setPerfil(PerfilUsuario.MOTORISTA);
            usuarioRepositorio.save(usuarioPorCpf);
        }

        return MembroInstituicaoResposta.converter(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembroInstituicaoResposta> listarMembros(Long instituicaoId, Usuario usuarioAutenticado) {
        Instituicao instituicao = buscarEValidarAdminMaster(instituicaoId, usuarioAutenticado);
        return membroRepositorio.findAllByInstituicaoAndAtivoTrueOrderByDataVinculoDesc(instituicao)
                .stream()
                .map(MembroInstituicaoResposta::converter)
                .toList();
    }

    @Override
    @Transactional
    public void desvincularMembro(Long instituicaoId, Long membroId, Usuario usuarioAutenticado) {
        Instituicao instituicao = buscarEValidarAdminMaster(instituicaoId, usuarioAutenticado);

        MembroInstituicao membro = membroRepositorio.findById(membroId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Vinculo de membro nao encontrado."));

        if (!membro.getInstituicao().getId().equals(instituicao.getId())) {
            throw new ExcecaoConflitoNegocio("Este membro nao pertence a instituicao informada.");
        }

        membro.setAtivo(false);
        membroRepositorio.save(membro);

        Usuario usuario = membro.getUsuario();
        if (membro.getPapel() == PapelMembroInstituicao.MOTORISTA
                && !membroRepositorio.existsByUsuarioIdAndPapelAndAtivoTrue(usuario.getId(), PapelMembroInstituicao.MOTORISTA)) {
            usuario.setPerfil(PerfilUsuario.USUARIO);
            usuarioRepositorio.save(usuario);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembroInstituicaoResposta> listarMeusVinculos(Usuario usuarioAutenticado) {
        return membroRepositorio.findAllByUsuarioAndAtivoTrue(usuarioAutenticado)
                .stream()
                .map(MembroInstituicaoResposta::converter)
                .toList();
    }

    private Instituicao buscarEValidarAdminMaster(Long instituicaoId, Usuario usuarioAutenticado) {
        Instituicao instituicao = instituicaoRepositorio.findById(instituicaoId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Instituicao nao encontrada."));

        boolean ehResponsavel = instituicao.getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId());
        boolean ehAdminGeral = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehResponsavel && !ehAdminGeral) {
            throw new ExcecaoConflitoNegocio("Apenas o Administrador Master da instituicao pode gerenciar membros.");
        }

        return instituicao;
    }
}