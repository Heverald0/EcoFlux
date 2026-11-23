package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.VincularMembroRequisicao;
import com.example.EcoFlux.dto.request.response.MembroInstituicaoResposta;
import com.example.EcoFlux.entity.Usuario;

import java.util.List;

public interface MembroInstituicaoServico {
    MembroInstituicaoResposta vincularMembro(Long instituicaoId, VincularMembroRequisicao requisicao, Usuario usuarioAutenticado);
    List<MembroInstituicaoResposta> listarMembros(Long instituicaoId, Usuario usuarioAutenticado);
    void desvincularMembro(Long instituicaoId, Long membroId, Usuario usuarioAutenticado);
    List<MembroInstituicaoResposta> listarMeusVinculos(Usuario usuarioAutenticado);
}