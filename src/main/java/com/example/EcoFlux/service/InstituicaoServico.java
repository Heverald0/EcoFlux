package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.InstituicaoCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;

import java.util.List;

public interface InstituicaoServico {
    InstituicaoResposta cadastrar(InstituicaoCadastroRequisicao requisicao, Usuario usuarioResponsavel);
    List<InstituicaoResposta> listarMinhasInstituicoes(Usuario usuarioResponsavel);
    InstituicaoResposta buscarPorId(Long id, Usuario usuarioAutenticado);
}