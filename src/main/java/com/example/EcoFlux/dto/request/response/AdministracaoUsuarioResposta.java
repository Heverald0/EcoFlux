package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.Instituicao;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusUsuario;
import java.time.LocalDateTime;
import java.util.List;

public record AdministracaoUsuarioResposta(Long id, String nome, String email, PerfilUsuario perfil,
        StatusUsuario status, LocalDateTime dataCriacao, List<InstituicaoResumo> instituicoes) {
    public static AdministracaoUsuarioResposta de(Usuario usuario, List<Instituicao> instituicoes) {
        return new AdministracaoUsuarioResposta(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                usuario.getStatus(), usuario.getDataCriacao(), instituicoes.stream()
                .map(i -> new InstituicaoResumo(i.getId(), i.getNome())).toList());
    }

    public record InstituicaoResumo(Long id, String nome) {}
}
