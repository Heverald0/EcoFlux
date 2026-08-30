package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.ColetaAgendamentoRequisicao;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.entity.Usuario;

import java.util.List;

public interface ColetaServico {
    ColetaResposta agendar(ColetaAgendamentoRequisicao requisicao, Usuario usuarioSolicitante);
    List<ColetaResposta> listarMinhasColetas(Usuario usuarioSolicitante);
    ColetaResposta buscarPorId(Long id, Usuario usuarioAutenticado);
    ColetaResposta cancelarColeta(Long id, Usuario usuarioAutenticado);
}