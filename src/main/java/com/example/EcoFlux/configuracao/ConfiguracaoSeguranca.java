package com.example.EcoFlux.configuracao;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.EcoFlux.exception.RespostaErro;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ConfiguracaoSeguranca {

    private final FiltroSeguranca filtroSeguranca;
    private final FiltroLimiteAcesso filtroLimiteAcesso;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8550}")
    private String origens;

    @Bean
    public PasswordEncoder codificadorSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager gerenciadorAutenticacao(AuthenticationConfiguration configuracao) throws Exception {
        return configuracao.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain cadeiaFiltrosSeguranca(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(autorizacao -> autorizacao
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers("/actuator/health", "/error", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated()
            )
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(res.getOutputStream(), new RespostaErro(401, "Unauthorized", "Autenticacao necessaria ou sessao expirada.", req.getRequestURI()));
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(res.getOutputStream(), new RespostaErro(403, "Forbidden", "Acesso negado.", req.getRequestURI()));
                }))
            .addFilterBefore(filtroSeguranca, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(filtroLimiteAcesso, FiltroSeguranca.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origens.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Content-Disposition", "Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<FiltroSeguranca> registroFiltroSeguranca() {
        FilterRegistrationBean<FiltroSeguranca> registro = new FilterRegistrationBean<>(filtroSeguranca);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public FilterRegistrationBean<FiltroLimiteAcesso> registroFiltroLimite() {
        FilterRegistrationBean<FiltroLimiteAcesso> registro = new FilterRegistrationBean<>(filtroLimiteAcesso);
        registro.setEnabled(false);
        return registro;
    }
}
