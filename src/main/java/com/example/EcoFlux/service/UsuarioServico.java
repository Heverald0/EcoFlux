package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.UsuarioCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;

public interface UsuarioServico {
    UsuarioResposta cadastrar(UsuarioCadastroRequisicao requisicao);
}