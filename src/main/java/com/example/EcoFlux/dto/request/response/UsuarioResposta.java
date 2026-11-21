package com.example.EcoFlux.dto.request.response;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;

import java.time.LocalDateTime;

public record UsuarioResposta(
        Long id,
        String nome,
        String email,
        String documento,
        String telefone,
        PerfilUsuario perfil,
        StatusCadastro status,
        String motivoRejeicao,
        LocalDateTime dataCriacao
) {
    public static UsuarioResposta aPartirDaEntidade(Usuario usuario) {
        return new UsuarioResposta(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getDocumento(),
                usuario.getTelefone(),
                usuario.getPerfil(),
                usuario.getStatus(),
                usuario.getMotivoRejeicao(),
                usuario.getDataCriacao()
        );
    }
}