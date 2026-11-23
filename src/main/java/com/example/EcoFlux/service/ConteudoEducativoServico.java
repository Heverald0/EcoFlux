package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.ConteudoEducativoRequisicao;
import com.example.EcoFlux.dto.request.response.ConteudoEducativoResposta;
import com.example.EcoFlux.dto.request.response.GuiaTriagemMaterialResposta;
import com.example.EcoFlux.entity.Usuario;

import java.util.List;

public interface ConteudoEducativoServico {
    List<ConteudoEducativoResposta> listarArtigos(String categoria);
    ConteudoEducativoResposta buscarPorSlug(String slug);
    List<GuiaTriagemMaterialResposta> listarGuiaTriagem();
    ConteudoEducativoResposta criarArtigo(ConteudoEducativoRequisicao requisicao, Usuario adminAutenticado);
    ConteudoEducativoResposta atualizarArtigo(Long id, ConteudoEducativoRequisicao requisicao, Usuario adminAutenticado);
    void alterarStatusAtivo(Long id, boolean ativo, Usuario adminAutenticado);
}