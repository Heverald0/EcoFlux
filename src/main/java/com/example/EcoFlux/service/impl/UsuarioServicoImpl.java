package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.UsuarioCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.exception.EmailJaCadastradoExcecao;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.service.UsuarioServico;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Usuario novoUsuario = Usuario.builder()
                .nome(requisicao.nome().trim())
                .email(emailFormatado)
                .senha(codificadorSenha.encode(requisicao.senha()))
                .perfil(PerfilUsuario.USUARIO)
                .build();

        Usuario usuarioSalvo = usuarioRepositorio.save(novoUsuario);
        return UsuarioResposta.aPartirDaEntidade(usuarioSalvo);
    }
}