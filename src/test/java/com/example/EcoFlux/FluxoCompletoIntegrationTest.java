package com.example.EcoFlux;

import com.example.EcoFlux.entity.Usuario;
import com.example.EcoFlux.entity.enums.PerfilUsuario;
import com.example.EcoFlux.repository.*;
import com.example.EcoFlux.configuracao.ServicoToken;
import com.fasterxml.jackson.databind.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.auth-limit=1000")
@AutoConfigureMockMvc @ActiveProfiles("test")
class FluxoCompletoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired RegistroMaterialRepositorio materiais;
    @Autowired MovimentoScoreRepositorio movimentos;
    @Autowired ServicoToken tokens;
    @Autowired PasswordEncoder encoder;

    @Test
    void cadastroAprovacaoRotaMaterialIndicadoresERelatorio() throws Exception {
        Usuario administrador = usuarios.saveAndFlush(Usuario.builder().nome("Administrador Teste")
                .email(UUID.randomUUID() + "@example.com").senha(encoder.encode("SenhaSegura123!"))
                .perfil(PerfilUsuario.ADMINISTRADOR).build());
        String admin = tokens.gerarToken(administrador);
        JsonNode empresa = cadastrar("EMPRESA");
        JsonNode cooperativa = cadastrar("COOPERATIVA");
        long empresaId = empresa.path("instituicao").path("id").asLong();
        long cooperativaId = cooperativa.path("instituicao").path("id").asLong();
        assertThat(empresaId).isPositive();
        for (long id : List.of(empresaId, cooperativaId)) {
            enviar("PATCH", "/api/v1/admin/instituicoes/" + id + "/status", admin,
                    Map.of("status", "ATIVO", "motivo", "Cadastro verificado"), 200);
        }
        String cliente = login(empresa.path("usuario").path("email").asText());
        String coletor = login(cooperativa.path("usuario").path("email").asText());
        Map<String, Object> ponto = new LinkedHashMap<>(endereco());
        ponto.putAll(Map.of("nome", "Ponto " + UUID.randomUUID(), "nomeResponsavel", "Maria Silva",
                "horarioFuncionamento", "08:00-18:00", "materiaisAceitos", List.of("PAPEL"),
                "latitude", -23.5505, "longitude", -46.6333, "instituicaoId", empresaId));
        long pontoId = enviar("POST", "/api/v1/pontos-coleta", cliente, ponto, 201).path("id").asLong();
        LocalDateTime partida = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        Map<String, Object> pedido = new LinkedHashMap<>(endereco());
        pedido.putAll(Map.of("dataHoraAgendamento", partida.toString(), "tipoMaterial", "PAPEL",
                "quantidadeEstimadaKg", 500, "unidade", "KG", "instituicaoId", empresaId,
                "cooperativaId", cooperativaId, "pontoColetaId", pontoId));
        long coletaId = enviar("POST", "/api/v1/coletas", cliente, pedido, 201).path("id").asLong();
        enviar("PATCH", "/api/v1/coletas/" + coletaId + "/status", cliente, Map.of("novoStatus", "AGENDADA"), 403);
        enviar("PATCH", "/api/v1/coletas/" + coletaId + "/status", coletor, Map.of("novoStatus", "REALIZADA", "quantidadeReal", 120), 409);
        enviar("PATCH", "/api/v1/coletas/" + coletaId + "/status", coletor, Map.of("novoStatus", "EM_ANALISE"), 200);
        enviar("PATCH", "/api/v1/coletas/" + coletaId + "/status", coletor, Map.of("novoStatus", "AGENDADA"), 200);
        Map<String, Object> plano = Map.of("nome", "Rota centro", "cooperativaId", cooperativaId,
                "partida", partida.toString(), "latitudeOrigem", -23.54, "longitudeOrigem", -46.62, "coletaIds", List.of(coletaId));
        JsonNode rota = enviar("POST", "/api/v1/rotas", coletor, plano, 201);
        long rotaId = rota.path("id").asLong();
        long paradaId = rota.path("paradas").get(0).path("id").asLong();
        enviar("POST", "/api/v1/rotas", coletor, plano, 409);
        enviar("PATCH", "/api/v1/coletas/" + coletaId + "/cancelar", cliente, Map.of(), 409);
        enviar("POST", "/api/v1/rotas/" + rotaId + "/otimizar", coletor, Map.of(), 200);
        enviar("POST", "/api/v1/rotas/" + rotaId + "/iniciar", coletor, Map.of(), 200);
        enviar("POST", "/api/v1/rotas/" + rotaId + "/paradas/" + paradaId + "/concluir", coletor, Map.of("quantidadeReal", 0), 400);
        JsonNode concluida = enviar("POST", "/api/v1/rotas/" + rotaId + "/paradas/" + paradaId + "/concluir", coletor,
                Map.of("quantidadeReal", 120, "observacao", "Pesagem confirmada"), 200);
        assertThat(concluida.path("status").asText()).isEqualTo("CONCLUIDA");
        assertThat(concluida.path("progressoPercentual").asInt()).isEqualTo(100);
        enviar("POST", "/api/v1/rotas/" + rotaId + "/paradas/" + paradaId + "/concluir", coletor, Map.of("quantidadeReal", 120), 409);
        var material = materiais.findByColetaId(coletaId).orElseThrow();
        assertThat(material.getQuantidade()).isEqualByComparingTo("120");
        assertThat(movimentos.existsByMaterialId(material.getId())).isTrue();
        String filtro = "?instituicaoId=" + empresaId;
        JsonNode painel = consultar("/api/v1/dashboard" + filtro, cliente, 200);
        assertThat(painel.path("materiais").path("quantidades").path("KG").decimalValue()).isEqualByComparingTo("120");
        assertThat(painel.path("coletasRealizadas").asLong()).isEqualTo(1);
        assertThat(painel.path("materiais").path("registrosSemFator").asLong()).isEqualTo(1);
        JsonNode pontuacao = consultar("/api/v1/ecoscore" + filtro, cliente, 200);
        assertThat(pontuacao.path("pontos").path("total").asLong()).isEqualTo(27);
        consultar("/api/v1/dashboard" + filtro, coletor, 403);
        consultar("/api/v1/coletas/operacional", cliente, 403);
        JsonNode timeline = consultar("/api/v1/coletas/" + coletaId + "/timeline", cliente, 200);
        assertThat(timeline.size()).isEqualTo(5);
        JsonNode notificacoes = consultar("/api/v1/notificacoes", cliente, 200);
        long notificacaoId = notificacoes.path("content").get(0).path("id").asLong();
        enviar("PATCH", "/api/v1/notificacoes/" + notificacaoId + "/lida", coletor, Map.of(), 404);
        enviar("PATCH", "/api/v1/notificacoes/" + notificacaoId + "/lida", cliente, Map.of(), 200);

        JsonNode relatorio = enviar("POST", "/api/v1/relatorios", cliente,
                Map.of("nome", "Resumo ambiental da instituicao", "tipo", "MATERIAIS", "instituicaoId", empresaId,
                        "inicio", LocalDate.now().minusMonths(4).withDayOfMonth(1).toString(), "fim", LocalDate.now().toString()), 201);
        long relatorioId = relatorio.path("id").asLong();
        assertThat(relatorio.path("status").asText()).isEqualTo("PRONTO");
        JsonNode snapshot = consultar("/api/v1/relatorios/" + relatorioId, cliente, 200);
        assertThat(snapshot.path("dados").path("resumo").path("quantidades").path("KG").decimalValue()).isEqualByComparingTo("120");
        consultar("/api/v1/relatorios/" + relatorioId, coletor, 403);
        byte[] arquivo = mvc.perform(get("/api/v1/relatorios/" + relatorioId + "/download").header("Authorization", "Bearer " + cliente))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn().getResponse().getContentAsByteArray();
        try (var documento = Loader.loadPDF(arquivo)) {
            String texto = new PDFTextStripper().getText(documento);
            assertThat(texto).contains("ECOFLUX", "120", "PAPEL", "COLETADO", "Registros sem fator");
            assertThat(documento.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
        Files.createDirectories(Path.of("target", "qa"));
        Files.write(Path.of("target", "qa", "relatorio-materiais.pdf"), arquivo);
        assertThat(consultar("/api/v1/relatorios/" + relatorioId, cliente, 200).path("relatorio").path("exportacoes").asInt()).isEqualTo(1);
        consultar("/v3/api-docs", null, 200);
    }

    private JsonNode cadastrar(String tipo) throws Exception {
        Map<String, Object> instituicao = new LinkedHashMap<>(endereco());
        instituicao.putAll(Map.of("nome", "Instituicao " + UUID.randomUUID(), "documento", cnpj(), "tipo", tipo, "telefone", "11999999999"));
        return enviar("POST", "/api/v1/auth/cadastro", null, Map.of("usuario", Map.of("nome", "Maria Silva", "email", UUID.randomUUID() + "@example.com",
                "senha", "SenhaSegura123!"), "confirmarSenha", "SenhaSegura123!", "instituicao", instituicao), 201);
    }
    private String login(String email) throws Exception {
        return enviar("POST", "/api/v1/auth/login", null, Map.of("email", email, "senha", "SenhaSegura123!"), 200).path("token").asText();
    }
    private Map<String, Object> endereco() {
        return Map.of("logradouro", "Rua das Flores", "numero", "10", "bairro", "Centro", "cidade", "Sao Paulo", "estado", "SP", "cep", "01001000");
    }
    private JsonNode enviar(String metodo, String caminho, String token, Object corpo, int esperado) throws Exception {
        var pedido = switch (metodo) { case "POST" -> post(caminho); case "PATCH" -> patch(caminho); default -> put(caminho); };
        if (token != null) pedido.header("Authorization", "Bearer " + token);
        String resposta = mvc.perform(pedido.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(corpo)))
                .andExpect(status().is(esperado)).andReturn().getResponse().getContentAsString();
        return resposta.isEmpty() ? mapper.createObjectNode() : mapper.readTree(resposta);
    }
    private JsonNode consultar(String caminho, String token, int esperado) throws Exception {
        var pedido = get(caminho);
        if (token != null) pedido.header("Authorization", "Bearer " + token);
        return mapper.readTree(mvc.perform(pedido).andExpect(status().is(esperado)).andReturn().getResponse().getContentAsString());
    }
    private String cnpj() {
        String base = String.format("%012d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 1000000000000L));
        for (int[] pesos : List.of(new int[]{5,4,3,2,9,8,7,6,5,4,3,2}, new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2})) {
            int soma = 0;
            for (int i = 0; i < pesos.length; i++) soma += (base.charAt(i) - '0') * pesos[i];
            int resto = soma % 11;
            base += resto < 2 ? "0" : Integer.toString(11 - resto);
        }
        return base;
    }
}
