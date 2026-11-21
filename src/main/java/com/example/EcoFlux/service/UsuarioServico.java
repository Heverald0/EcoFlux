package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.AtualizarStatusCadastroRequisicao;
import com.example.EcoFlux.dto.request.UsuarioCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;

import java.util.List;

public interface UsuarioServico {
    UsuarioResposta cadastrar(UsuarioCadastroRequisicao requisicao);
    List<UsuarioResposta> listar(StatusCadastro status);
    UsuarioResposta buscarPorId(Long id);
    UsuarioResposta atualizarStatus(Long id, AtualizarStatusCadastroRequisicao requisicao, Usuario adminAutenticado);
}