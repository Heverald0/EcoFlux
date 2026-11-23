package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.AtualizarStatusCadastroRequisicao;
import com.example.EcoFlux.dto.request.UsuarioCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.exception.EmailJaCadastradoExcecao;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.service.UsuarioServico;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServicoImpl implements UsuarioServico {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificadorSenha;

    @Override
    @Transactional
    public UsuarioResposta cadastrar(UsuarioCadastroRequisicao requisicao) {
        String emailFormatado = requisicao.email().trim().toLowerCase();

        if (usuarioRepositorio.existsByEmail(emailFormatado)) {
            throw new EmailJaCadastradoExcecao(emailFormatado);
        }

        String documentoLimpo = (requisicao.documento() != null && !requisicao.documento().isBlank())
                ? requisicao.documento().replaceAll("[^0-9]", "").trim()
                : null;

        if (documentoLimpo != null && usuarioRepositorio.existsByDocumento(documentoLimpo)) {
            throw new ExcecaoConflitoNegocio(String.format("Ja existe um usuario cadastrado com o documento '%s'.", requisicao.documento()));
        }

        Usuario novoUsuario = Usuario.builder()
                .nome(requisicao.nome().trim())
                .email(emailFormatado)
                .documento(documentoLimpo)
                .telefone(requisicao.telefone() != null ? requisicao.telefone().trim() : null)
                .senha(codificadorSenha.encode(requisicao.senha()))
                .perfil(PerfilUsuario.USUARIO)
                .status(StatusCadastro.ATIVO)
                .build();

        Usuario usuarioSalvo = usuarioRepositorio.save(novoUsuario);
        return UsuarioResposta.aPartirDaEntidade(usuarioSalvo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResposta> listar(StatusCadastro status) {
        List<Usuario> usuarios = (status != null)
                ? usuarioRepositorio.findAllByStatusOrderByDataCriacaoDesc(status)
                : usuarioRepositorio.findAllByOrderByDataCriacaoDesc();

        return usuarios.stream().map(UsuarioResposta::aPartirDaEntidade).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResposta buscarPorId(Long id) {
        Usuario usuario = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Usuario com ID %d nao encontrado.", id)));
        return UsuarioResposta.aPartirDaEntidade(usuario);
    }

    @Override
    @Transactional
    public UsuarioResposta atualizarStatus(Long id, AtualizarStatusCadastroRequisicao requisicao, Usuario adminAutenticado) {
        if (adminAutenticado.getPerfil() != PerfilUsuario.ADMINISTRADOR) {
            throw new ExcecaoConflitoNegocio("Apenas administradores podem alterar o status de usuarios.");
        }

        Usuario usuario = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Usuario com ID %d nao encontrado.", id)));

        if (requisicao.status() == StatusCadastro.REJEITADO
                && (requisicao.motivo() == null || requisicao.motivo().isBlank())) {
            throw new ExcecaoConflitoNegocio("Informe o motivo ao rejeitar um cadastro.");
        }

        usuario.setStatus(requisicao.status());
        usuario.setMotivoRejeicao(requisicao.status() == StatusCadastro.REJEITADO
                ? requisicao.motivo().trim()
                : null);

        return UsuarioResposta.aPartirDaEntidade(usuarioRepositorio.save(usuario));
    }
}