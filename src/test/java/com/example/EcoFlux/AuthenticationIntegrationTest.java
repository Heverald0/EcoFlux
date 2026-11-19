package com.example.EcoFlux;

import com.example.EcoFlux.configuracao.ServicoToken;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.entity.enums.StatusUsuario;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.auth-limit=1000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired ServicoToken tokens;

    @Test
    void cadastroLoginPerfilELogout() throws Exception {
        String email = email();
        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("usuario", Map.of("nome", "Ana Silva", "email", email, "senha", "SenhaSegura123!"),
                        "confirmarSenha", "SenhaSegura123!", "cpf", "52998224725"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("usuario.status").value("ATIVO"));
        String resposta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "senha", "SenhaSegura123!"))))
                .andExpect(status().isOk()).andExpect(jsonPath("usuario.perfil").value("USUARIO"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(resposta).get("token").asText();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("email").value(email)).andExpect(jsonPath("senha").doesNotExist());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroInstitucionalInvalidoNaoDeixaContaParcial() throws Exception {
        String email = email();
        var instituicao = Map.ofEntries(Map.entry("nome", "Cooperativa Teste"), Map.entry("documento", "11111111111111"),
                Map.entry("tipo", "COOPERATIVA"), Map.entry("telefone", "11999999999"), Map.entry("logradouro", "Rua Teste"),
                Map.entry("numero", "1"), Map.entry("bairro", "Centro"), Map.entry("cidade", "Sao Paulo"), Map.entry("estado", "SP"),
                Map.entry("cep", "01001000"));
        mvc.perform(post("/api/v1/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("usuario", Map.of("nome", "Ana Silva", "email", email, "senha", "SenhaSegura123!"),
                        "confirmarSenha", "SenhaSegura123!", "instituicao", instituicao))))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.existsByEmail(email)).isFalse();
    }

    @Test
    void contaBloqueadaPerdeAcessoMesmoComTokenValido() throws Exception {
        Usuario usuario = conta(PerfilUsuario.USUARIO);
        String token = tokens.gerarToken(usuario);
        usuario.setStatus(StatusUsuario.BLOQUEADO);
        usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", usuario.getEmail(), "senha", "SenhaSegura123!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioComumNaoAcessaAdministracao() throws Exception {
        String token = tokens.gerarToken(conta(PerfilUsuario.USUARIO));
        mvc.perform(get("/api/v1/admin/usuarios").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/resumo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer token-invalido")).andExpect(status().isUnauthorized());
    }

    @Test
    void administradorPodeBloquearOutraContaSemRemoverProprioAcesso() throws Exception {
        Usuario administrador = conta(PerfilUsuario.ADMINISTRADOR);
        Usuario usuario = conta(PerfilUsuario.USUARIO);
        String token = tokens.gerarToken(administrador);
        mvc.perform(patch("/api/v1/admin/usuarios/" + usuario.getId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOQUEADO\",\"motivo\":\"Revisao cadastral\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("BLOQUEADO"));
        mvc.perform(patch("/api/v1/admin/usuarios/" + administrador.getId()).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"perfil\":\"USUARIO\",\"motivo\":\"Teste\"}"))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.findById(administrador.getId()).orElseThrow().getPerfil()).isEqualTo(PerfilUsuario.ADMINISTRADOR);
    }

    private Usuario conta(PerfilUsuario perfil) {
        return usuarios.saveAndFlush(Usuario.builder().nome("Pessoa Teste").email(email()).senha(encoder.encode("SenhaSegura123!"))
                .perfil(perfil).build());
    }

    private String email() { return UUID.randomUUID() + "@example.com"; }
}
