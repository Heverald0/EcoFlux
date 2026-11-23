package com.example.EcoFlux.configuracao;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusCadastro;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class InicializadorAdminConfig {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner garantirAdministradorPadrao() {
        return args -> {
            String emailAdmin = "admin@ecoflux.com";

            usuarioRepositorio.findByEmail(emailAdmin).ifPresentOrElse(
                    existente -> {
                        existente.setSenha(passwordEncoder.encode("Admin@123"));
                        existente.setPerfil(PerfilUsuario.ADMINISTRADOR);
                        existente.setStatus(StatusCadastro.ATIVO);
                        usuarioRepositorio.save(existente);
                    },
                    () -> {
                        Usuario admin = Usuario.builder()
                                .nome("Administrador EcoFlux")
                                .email(emailAdmin)
                                .documento("00000000000")
                                .telefone("73999999999")
                                .senha(passwordEncoder.encode("Admin@123"))
                                .perfil(PerfilUsuario.ADMINISTRADOR)
                                .status(StatusCadastro.ATIVO)
                                .build();
                        usuarioRepositorio.save(admin);
                    }
            );
        };
    }
}