package com.example.EcoFlux;

import com.example.EcoFlux.configuracao.ServicoToken;
import com.example.EcoFlux.educacao.ProgressoConteudoRepositorio;
import com.example.EcoFlux.educacao.TentativaQuizRepositorio;
import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.repository.UsuarioRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.auth-limit=1000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EducacaoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired ServicoToken tokens;
    @Autowired ProgressoConteudoRepositorio progressos;
    @Autowired TentativaQuizRepositorio tentativas;

    @Test
    void quizAvaliaNoServidorPersisteProgressoENaoPermiteConclusaoDuplicada() throws Exception {
        Usuario administrador = usuario(PerfilUsuario.ADMINISTRADOR);
        Usuario aluno = usuario(PerfilUsuario.USUARIO);
        String adminToken = tokens.gerarToken(administrador);
        String token = tokens.gerarToken(aluno);
        var pedido = conteudo("QUIZ", List.of(Map.of("enunciado", "Onde descartar papel limpo?",
                "alternativas", List.of("Coleta seletiva", "Esgoto"), "alternativaCorreta", 0,
                "explicacao", "Papel limpo pode ser destinado a coleta seletiva.")));
        mvc.perform(post("/api/v1/educacao/admin/conteudos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(pedido)))
                .andExpect(status().isForbidden());
        JsonNode criado = mapper.readTree(mvc.perform(post("/api/v1/educacao/admin/conteudos")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(pedido))).andExpect(status().isCreated())
                .andExpect(jsonPath("perguntas[0].alternativaCorreta").doesNotExist())
                .andReturn().getResponse().getContentAsString());
        long id = criado.get("id").asLong();
        long perguntaId = criado.get("perguntas").get(0).get("id").asLong();
        String base = "/api/v1/educacao/conteudos/" + id;
        mvc.perform(put(base + "/progresso").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"percentual\":100}"))
                .andExpect(status().isConflict());
        mvc.perform(post(base + "/respostas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(respostas(perguntaId, 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("aprovado").value(false))
                .andExpect(jsonPath("pontosGerados").value(0));
        mvc.perform(post(base + "/respostas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(respostas(perguntaId, 0)))
                .andExpect(status().isOk()).andExpect(jsonPath("aprovado").value(true))
                .andExpect(jsonPath("acertos").value(1));
        mvc.perform(post(base + "/respostas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(respostas(perguntaId, 0)))
                .andExpect(status().isConflict());
        assertThat(tentativas.countByUsuarioIdAndConteudoId(aluno.getId(), id)).isEqualTo(2);
        assertThat(progressos.findByUsuarioIdAndConteudoId(aluno.getId(), id).orElseThrow().getPercentual()).isEqualTo(100);
        mvc.perform(delete("/api/v1/educacao/admin/conteudos/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mvc.perform(get(base).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
    }

    @Test
    void guiaDisponibilizaSomenteArquivoPersistidoValido() throws Exception {
        String token = tokens.gerarToken(usuario(PerfilUsuario.ADMINISTRADOR));
        JsonNode criado = mapper.readTree(mvc.perform(post("/api/v1/educacao/admin/conteudos")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(conteudo("GUIA", List.of())))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        long id = criado.get("id").asLong();
        String base = "/api/v1/educacao/conteudos/" + id;
        mvc.perform(get(base + "/download").header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/v1/educacao/admin/conteudos/" + id + "/arquivo")
                .file(new MockMultipartFile("arquivo", "falso.pdf", "application/pdf", "texto".getBytes(StandardCharsets.UTF_8)))
                .with(request -> { request.setMethod("PUT"); return request; }).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/educacao/admin/conteudos/" + id + "/arquivo")
                .file(new MockMultipartFile("arquivo", "quebrado.pdf", "application/pdf", "%PDF-1.4\nquebrado".getBytes(StandardCharsets.US_ASCII)))
                .with(request -> { request.setMethod("PUT"); return request; }).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        byte[] pdf;
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            documento.addPage(new PDPage());
            documento.save(saida);
            pdf = saida.toByteArray();
        }
        mvc.perform(multipart("/api/v1/educacao/admin/conteudos/" + id + "/arquivo")
                .file(new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", pdf))
                .with(request -> { request.setMethod("PUT"); return request; }).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get(base + "/arquivo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().bytes(pdf))
                .andExpect(header().string("Content-Disposition", "attachment; filename=guia-" + id + ".pdf"));
    }

    private Usuario usuario(PerfilUsuario perfil) {
        return usuarios.saveAndFlush(Usuario.builder().nome("Pessoa Teste").email(UUID.randomUUID() + "@example.com")
                .senha("hash-teste").perfil(perfil).build());
    }

    private Map<String, Object> conteudo(String tipo, List<?> perguntas) {
        return Map.of("titulo", "Separacao de residuos", "descricao", "Aprenda a separar materiais.",
                "tipo", tipo, "publicado", true, "ordem", 1, "destaque", true, "perguntas", perguntas);
    }

    private byte[] respostas(long perguntaId, int alternativa) throws Exception {
        return mapper.writeValueAsBytes(Map.of("respostas", List.of(Map.of("perguntaId", perguntaId, "alternativa", alternativa))));
    }
}
