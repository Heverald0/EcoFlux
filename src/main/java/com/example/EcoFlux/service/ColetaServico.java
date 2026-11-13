package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.AtualizarStatusColetaRequisicao;
import com.example.EcoFlux.dto.request.ColetaAgendamentoRequisicao;
import com.example.EcoFlux.dto.request.response.ColetaResposta;
import com.example.EcoFlux.dto.request.response.EventoTimelineColetaResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusColeta;

import java.util.List;

public interface ColetaServico {
    ColetaResposta agendar(ColetaAgendamentoRequisicao requisicao, Usuario usuarioSolicitante);
    List<ColetaResposta> listarMinhasColetas(Usuario usuarioSolicitante, StatusColeta status);
    List<ColetaResposta> listarPorStatusOperacional(StatusColeta status);
    ColetaResposta buscarPorId(Long id, Usuario usuarioAutenticado);
    ColetaResposta atualizarStatus(Long id, AtualizarStatusColetaRequisicao requisicao, Usuario usuarioAutenticado);
    ColetaResposta cancelarColeta(Long id, Usuario usuarioAutenticado);
    List<EventoTimelineColetaResposta> consultarTimeline(Long coletaId, Usuario usuarioAutenticado);
}