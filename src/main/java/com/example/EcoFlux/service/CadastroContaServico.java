package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.CadastroContaRequisicao;
import com.example.EcoFlux.dto.request.response.CadastroContaResposta;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRegraNegocio;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.validacao.DocumentoBrasileiro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CadastroContaServico {
    private final UsuarioServico usuarioServico;
    private final InstituicaoServico instituicaoServico;
    private final UsuarioRepositorio usuarios;

    @Transactional
    public CadastroContaResposta cadastrar(CadastroContaRequisicao requisicao) {
        if (!requisicao.usuario().senha().equals(requisicao.confirmarSenha())) {
            throw new ExcecaoRegraNegocio("confirmarSenha: as senhas informadas nao coincidem.");
        }
        String cpf = DocumentoBrasileiro.normalizar(requisicao.cpf());
        if (!cpf.isEmpty()) {
            if (!DocumentoBrasileiro.cpfValido(cpf)) throw new ExcecaoRegraNegocio("cpf: documento invalido.");
            if (usuarios.existsByDocumento(cpf)) throw new ExcecaoConflitoNegocio("CPF ja cadastrado.");
        }
        UsuarioResposta conta = usuarioServico.cadastrar(requisicao.usuario());
        Usuario usuario = usuarios.findById(conta.id()).orElseThrow();
        usuario.setDocumento(cpf.isEmpty() ? null : cpf);
        InstituicaoResposta instituicao = requisicao.instituicao() == null ? null
                : instituicaoServico.cadastrar(requisicao.instituicao(), usuario);
        usuarios.save(usuario);
        return new CadastroContaResposta(UsuarioResposta.aPartirDaEntidade(usuario), instituicao);
    }
}
