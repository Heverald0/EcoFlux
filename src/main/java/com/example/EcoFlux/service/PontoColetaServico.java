package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.AlterarStatusPontoRequisicao;
import com.example.EcoFlux.dto.request.PontoColetaRequisicao;
import com.example.EcoFlux.dto.request.response.PontoColetaResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusPontoColeta;
import com.example.EcoFlux.entity.enums.TipoMaterial;

import java.util.List;

public interface PontoColetaServico {
    PontoColetaResposta cadastrar(PontoColetaRequisicao requisicao, Usuario usuarioAutenticado);
    PontoColetaResposta atualizar(Long id, PontoColetaRequisicao requisicao, Usuario usuarioAutenticado);
    PontoColetaResposta alterarStatus(Long id, AlterarStatusPontoRequisicao requisicao, Usuario usuarioAutenticado);
    PontoColetaResposta buscarPorId(Long id);
    List<PontoColetaResposta> listarComFiltros(StatusPontoColeta status, String bairro, TipoMaterial material);
}