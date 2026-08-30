package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;

import java.time.LocalDateTime;

public record UsuarioResposta(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        LocalDateTime dataCriacao
) {
    public static UsuarioResposta aPartirDaEntidade(Usuario usuario) {
        return new UsuarioResposta(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getDataCriacao()
        );
    }
}