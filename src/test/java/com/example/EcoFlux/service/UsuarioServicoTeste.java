package com.example.EcoFlux.service;

import com.example.EcoFlux.dto.request.UsuarioCadastroRequisicao;
import com.example.EcoFlux.dto.request.response.UsuarioResposta;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.exception.EmailJaCadastradoExcecao;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.example.EcoFlux.service.impl.UsuarioServicoImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServicoTeste {

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    @Mock
    private PasswordEncoder codificadorSenha;

    @InjectMocks
    private UsuarioServicoImpl usuarioServico;

    @Test
    @DisplayName("Deve cadastrar usuario com sucesso quando os dados forem validos")
    void deveCadastrarUsuarioComSucesso() {
        UsuarioCadastroRequisicao requisicao = new UsuarioCadastroRequisicao(
                "Carlos Silva",
                "carlos@ecoflux.com",
                "SenhaForte@123"
        );

        Usuario usuarioSalvo = Usuario.builder()
                .id(1L)
                .nome("Carlos Silva")
                .email("carlos@ecoflux.com")
                .senha("senha_criptografada")
                .perfil(PerfilUsuario.USUARIO)
                .dataCriacao(LocalDateTime.now())
                .build();

        when(usuarioRepositorio.existsByEmail("carlos@ecoflux.com")).thenReturn(false);
        when(codificadorSenha.encode(requisicao.senha())).thenReturn("senha_criptografada");
        when(usuarioRepositorio.save(any(Usuario.class))).thenReturn(usuarioSalvo);

        UsuarioResposta resposta = usuarioServico.cadastrar(requisicao);

        assertNotNull(resposta);
        assertEquals(1L, resposta.id());
        assertEquals("carlos@ecoflux.com", resposta.email());
        assertEquals(PerfilUsuario.USUARIO, resposta.perfil());
        verify(usuarioRepositorio, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lancar EmailJaCadastradoExcecao quando email ja existir")
    void deveLancarExcecaoQuandoEmailJaExistir() {
        UsuarioCadastroRequisicao requisicao = new UsuarioCadastroRequisicao(
                "Carlos Silva",
                "carlos@ecoflux.com",
                "SenhaForte@123"
        );

        when(usuarioRepositorio.existsByEmail("carlos@ecoflux.com")).thenReturn(true);

        assertThrows(EmailJaCadastradoExcecao.class, () -> usuarioServico.cadastrar(requisicao));
        verify(usuarioRepositorio, never()).save(any(Usuario.class));
    }
}