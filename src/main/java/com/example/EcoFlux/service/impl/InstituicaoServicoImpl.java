package com.example.EcoFlux.service.impl;

import com.example.EcoFlux.dto.request.InstituicaoCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.exception.ExcecaoConflitoNegocio;
import com.example.EcoFlux.exception.ExcecaoRecursoNaoEncontrado;
import com.example.EcoFlux.repository.InstituicaoRepositorio;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.service.InstituicaoServico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstituicaoServicoImpl implements InstituicaoServico {

    private final InstituicaoRepositorio instituicaoRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    @Override
    @Transactional
    public InstituicaoResposta cadastrar(InstituicaoCadastroRequisicao requisicao, Usuario usuarioResponsavel) {
        String documentoLimpo = requisicao.documento().replaceAll("[^0-9A-Za-z]", "").trim();

        if (instituicaoRepositorio.existsByDocumento(documentoLimpo)) {
            throw new ExcecaoConflitoNegocio(String.format("Ja existe uma instituicao cadastrada com o documento '%s'.", requisicao.documento()));
        }

        Instituicao novaInstituicao = Instituicao.builder()
                .nome(requisicao.nome().trim())
                .documento(documentoLimpo)
                .tipo(requisicao.tipo())
                .telefone(requisicao.telefone().trim())
                .logradouro(requisicao.logradouro().trim())
                .numero(requisicao.numero().trim())
                .bairro(requisicao.bairro().trim())
                .cidade(requisicao.cidade().trim())
                .estado(requisicao.estado().trim().toUpperCase())
                .cep(requisicao.cep().trim())
                .usuarioResponsavel(usuarioResponsavel)
                .build();

        Instituicao instituicaoSalva = instituicaoRepositorio.save(novaInstituicao);

        if (usuarioResponsavel.getPerfil() == PerfilUsuario.USUARIO) {
            usuarioResponsavel.setPerfil(PerfilUsuario.INSTITUICAO);
            usuarioRepositorio.save(usuarioResponsavel);
        }

        return InstituicaoResposta.converter(instituicaoSalva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstituicaoResposta> listarMinhasInstituicoes(Usuario usuarioResponsavel) {
        return instituicaoRepositorio.findAllByUsuarioResponsavel(usuarioResponsavel)
                .stream()
                .map(InstituicaoResposta::converter)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstituicaoResposta buscarPorId(Long id, Usuario usuarioAutenticado) {
        Instituicao instituicao = instituicaoRepositorio.findById(id)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado(String.format("Instituicao com ID %d nao encontrada.", id)));

        boolean ehResponsavel = instituicao.getUsuarioResponsavel().getId().equals(usuarioAutenticado.getId());
        boolean ehAdmin = usuarioAutenticado.getPerfil() == PerfilUsuario.ADMINISTRADOR;

        if (!ehResponsavel && !ehAdmin) {
            throw new ExcecaoConflitoNegocio("Acesso negado: voce nao tem permissao para acessar os dados desta instituicao.");
        }

        return InstituicaoResposta.converter(instituicao);
    }
}