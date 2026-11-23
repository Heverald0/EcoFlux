package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.response.EcoImpactoResposta;
import com.example.EcoFlux.dto.request.response.EcoScoreResposta;
import com.example.EcoFlux.dto.request.response.EcoSeloResposta;
import com.example.EcoFlux.entity.Usuario;

import java.util.List;

public interface SustentabilidadeServico {
    EcoImpactoResposta calcularImpactoGlobal();
    EcoImpactoResposta calcularImpactoUsuario(Usuario usuario);
    EcoImpactoResposta calcularImpactoInstituicao(Long instituicaoId, Usuario usuarioAutenticado);
    EcoScoreResposta calcularEcoScoreUsuario(Usuario usuario);
    EcoScoreResposta calcularEcoScoreInstituicao(Long instituicaoId, Usuario usuarioAutenticado);
    List<EcoScoreResposta> listarRankingEcoScore();
    List<EcoSeloResposta> listarSelosUsuario(Usuario usuario);
    List<EcoSeloResposta> listarCatalogoSelos();
}