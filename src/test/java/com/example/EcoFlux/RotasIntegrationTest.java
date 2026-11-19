package com.example.EcoFlux;

import com.example.EcoFlux.configuracao.ServicoToken;
import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.repository.*;
import com.example.EcoFlux.rotas.DistanciaGeografica;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.auth-limit=1000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RotasIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired InstituicaoRepositorio instituicoes;
    @Autowired PontoColetaRepositorio pontos;
    @Autowired ColetaRepositorio coletas;
    @Autowired ServicoToken tokens;

    @Test
    void rotaReservaColetasOtimizaEExecutaParadasSemDuplicarConclusao() throws Exception {
        Usuario responsavel = usuario(PerfilUsuario.COOPERATIVA);
        Usuario solicitante = usuario(PerfilUsuario.USUARIO);
        Instituicao cooperativa = instituicao(responsavel, TipoInstituicao.COOPERATIVA);
        Instituicao origem = instituicao(solicitante, TipoInstituicao.EMPRESA);
        Coleta distante = coleta(solicitante, origem, cooperativa, ponto(responsavel, "-23.60"));
        Coleta proxima = coleta(solicitante, origem, cooperativa, ponto(responsavel, "-23.51"));
        String token = tokens.gerarToken(responsavel);
        var pedido = Map.of("nome", "Rota Centro", "cooperativaId", cooperativa.getId(),
                "partida", distante.getDataHoraAgendamento().toString(), "latitudeOrigem", -23.50,
                "longitudeOrigem", -46.60, "coletaIds", List.of(distante.getId(), proxima.getId()));
        JsonNode planejada = mapper.readTree(mvc.perform(post("/api/v1/rotas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(pedido)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id = planejada.get("id").asLong();
        String base = "/api/v1/rotas/" + id;
        mvc.perform(post("/api/v1/rotas").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(pedido)))
                .andExpect(status().isConflict());
        mvc.perform(get(base).header("Authorization", "Bearer " + tokens.gerarToken(solicitante)))
                .andExpect(status().isForbidden());
        JsonNode otimizada = mapper.readTree(mvc.perform(post(base + "/otimizar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("OTIMIZADA"))
                .andExpect(jsonPath("paradas[0].coletaId").value(proxima.getId())).andReturn().getResponse().getContentAsString());
        long primeira = otimizada.get("paradas").get(0).get("id").asLong();
        long segunda = otimizada.get("paradas").get(1).get("id").asLong();
        mvc.perform(post(base + "/iniciar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("paradas[0].status").value("EM_ROTA"));
        mvc.perform(post(base + "/paradas/" + segunda + "/concluir").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidadeReal\":8.5}"))
                .andExpect(status().isConflict());
        mvc.perform(post(base + "/paradas/" + primeira + "/concluir").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidadeReal\":8.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("progressoPercentual").value(50));
        mvc.perform(post(base + "/paradas/" + primeira + "/concluir").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantidadeReal\":8.5}"))
                .andExpect(status().isConflict());
        mvc.perform(post(base + "/paradas/" + segunda + "/falhar").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Local fechado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("CONCLUIDA"))
                .andExpect(jsonPath("progressoPercentual").value(100));
        assertThat(coletas.findById(proxima.getId()).orElseThrow().getStatus()).isEqualTo(StatusColeta.REALIZADA);
        assertThat(coletas.findById(proxima.getId()).orElseThrow().getQuantidadeReal()).isEqualByComparingTo("8.5");
        assertThat(coletas.findById(distante.getId()).orElseThrow().getStatus()).isEqualTo(StatusColeta.CANCELADA);
    }

    @Test
    void distanciaGeograficaTrataCoincidenciaEAntipodasSemErroNumerico() {
        assertThat(DistanciaGeografica.km(-23, -46, -23, -46)).isZero();
        assertThat(DistanciaGeografica.km(0, 0, 0, 180)).isBetween(20015.0, 20016.0);
    }

    private Usuario usuario(PerfilUsuario perfil) {
        return usuarios.saveAndFlush(Usuario.builder().nome("Pessoa Teste").email(UUID.randomUUID() + "@example.com")
                .senha("hash-teste").perfil(perfil).build());
    }

    private Instituicao instituicao(Usuario usuario, TipoInstituicao tipo) {
        return instituicoes.saveAndFlush(Instituicao.builder().nome("Instituicao Teste")
                .documento(UUID.randomUUID().toString().replace("-", "").substring(0, 14)).tipo(tipo)
                .status(StatusInstituicao.ATIVO).telefone("11999999999").logradouro("Rua Teste").numero("1")
                .bairro("Centro").cidade("Sao Paulo").estado("SP").cep("01001000").usuarioResponsavel(usuario).build());
    }

    private PontoColeta ponto(Usuario usuario, String latitude) {
        return pontos.saveAndFlush(PontoColeta.builder().nome(UUID.randomUUID().toString()).nomeResponsavel("Responsavel")
                .horarioFuncionamento("08:00-18:00").logradouro("Rua Teste").numero("1").bairro("Centro")
                .cidade("Sao Paulo").estado("SP").cep("01001000").latitude(new BigDecimal(latitude))
                .longitude(new BigDecimal("-46.60")).status(StatusPontoColeta.ATIVO).materiaisAceitos(Set.of(TipoMaterial.PAPEL))
                .usuarioCadastro(usuario).build());
    }

    private Coleta coleta(Usuario solicitante, Instituicao origem, Instituicao cooperativa, PontoColeta ponto) {
        return coletas.saveAndFlush(Coleta.builder().usuarioSolicitante(solicitante).instituicao(origem).cooperativa(cooperativa)
                .pontoColeta(ponto).dataHoraAgendamento(LocalDateTime.now().plusDays(1).withNano(0)).tipoMaterial(TipoMaterial.PAPEL)
                .quantidadeEstimadaKg(BigDecimal.TEN).status(StatusColeta.AGENDADA).logradouro("Rua Teste").numero("1")
                .bairro("Centro").cidade("Sao Paulo").estado("SP").cep("01001000").build());
    }
}
