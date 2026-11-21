package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.AtualizarStatusCadastroRequisicao;
import com.example.EcoFlux.dto.request.InstituicaoCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.InstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.entity.enums.TipoInstituicao;

import java.util.List;

public interface InstituicaoServico {
    InstituicaoResposta cadastrar(InstituicaoCadastroRequisicao requisicao, Usuario usuarioResponsavel);
    List<InstituicaoResposta> listarMinhasInstituicoes(Usuario usuarioResponsavel);
    List<InstituicaoResposta> listarComFiltros(TipoInstituicao tipo, StatusCadastro status);
    InstituicaoResposta buscarPorId(Long id, Usuario usuarioAutenticado);
    InstituicaoResposta atualizarStatus(Long id, AtualizarStatusCadastroRequisicao requisicao, Usuario adminAutenticado);
}