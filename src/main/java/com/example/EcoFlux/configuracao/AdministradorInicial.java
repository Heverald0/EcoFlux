package com.example.EcoFlux.configuracao;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AdministradorInicial implements ApplicationRunner {
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder senhaEncoder;
    @Value("${app.bootstrap.admin-email:}")
    private String email;
    @Value("${app.bootstrap.admin-password:}")
    private String senha;

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (email.isBlank() && senha.isBlank()) return;
        if (usuarios.existsByPerfil(PerfilUsuario.ADMINISTRADOR)) return;
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || senha.length() < 12
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configure um email valido e senha administrativa entre 12 caracteres e 72 bytes.");
        }
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarios.existsByEmail(emailNormalizado)) throw new IllegalStateException("A conta de bootstrap ja existe e nao e administradora.");
        usuarios.save(Usuario.builder().nome("Administrador").email(emailNormalizado)
                .senha(senhaEncoder.encode(senha)).perfil(PerfilUsuario.ADMINISTRADOR).build());
    }
}
