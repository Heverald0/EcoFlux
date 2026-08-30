package com.example.EcoFlux.configuracao;

import com.example.EcoFlux.repository.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ServicoAutenticacao implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepositorio.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado com o e-mail: " + username));
    }
}