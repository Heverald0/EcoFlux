package com.example.EcoFlux;

import com.example.EcoFlux.configuracao.ServicoToken;
import com.example.EcoFlux.entity.*;
import com.example.EcoFlux.entity.enums.*;
import com.example.EcoFlux.repository.*;
import com.example.EcoFlux.service.MaterialServico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.security.auth-limit=1000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AmbientalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired InstituicaoRepositorio instituicoes;
    @Autowired ColetaRepositorio coletas;
    @Autowired RegistroMaterialRepositorio materiais;
    @Autowired MovimentoScoreRepositorio movimentos;
    @Autowired CertificadoAmbientalRepositorio certificados;
    @Autowired ServicoToken tokens;
    @Autowired MaterialServico materialServico;
    Usuario dono;
    Usuario admin;
    Instituicao instituicao;
    String tokenDono;
    String tokenAdmin;

    @BeforeEach
    void preparar() {
        dono = usuario(PerfilUsuario.INSTITUICAO);
        admin = usuario(PerfilUsuario.ADMINISTRADOR);
        tokenDono = tokens.gerarToken(dono);
        tokenAdmin = tokens.gerarToken(admin);
        instituicao = instituicoes.saveAndFlush(Instituicao.builder().nome("Instituicao ambiental")
                .documento(UUID.randomUUID().toString().substring(0, 18)).tipo(TipoInstituicao.EMPRESA)
                .telefone("11999999999").logradouro("Rua Um").numero("1").bairro("Centro")
                .cidade("Sao Paulo").estado("SP").cep("01001000").status(StatusInstituicao.ATIVO)
                .usuarioResponsavel(dono).build());
    }

    @Test
    void recebimentoAlimentaIndicadoresScoreESeloSemDuplicacao() throws Exception {
        fator("teste-1", 2);
        long id = cadastrar("PAPEL", "KG", 100);
        alterar(id, "TRIAGEM", tokenDono, 403);
        alterar(id, "TRIAGEM", tokenAdmin, 200);
        alterar(id, "COLETADO", tokenAdmin, 200);
        alterar(id, "COMERCIALIZADO", tokenAdmin, 200);
        alterar(id, "COLETADO", tokenAdmin, 409);
        mvc.perform(get("/api/v1/ecoimpacto").param("instituicaoId", instituicao.getId().toString())
                .header("Authorization", "Bearer " + tokenDono)).andExpect(status().isOk())
                .andExpect(jsonPath("quantidades.KG").value(100)).andExpect(jsonPath("registrosConcluidos").value(1))
                .andExpect(jsonPath("impacto.co2Kg").value(200)).andExpect(jsonPath("registrosSemFator").value(0));
        mvc.perform(get("/api/v1/ecoscore").param("instituicaoId", instituicao.getId().toString())
                .header("Authorization", "Bearer " + tokenDono)).andExpect(status().isOk())
                .andExpect(jsonPath("pontos.total").value(25));
        assertThat(movimentos.findByInstituicaoIdOrderByDataCriacaoDesc(instituicao.getId())).hasSize(1);
        assertThat(certificados.findByInstituicaoIdOrderByDataCriacaoDescIdDesc(instituicao.getId())).hasSize(1);
        fator("teste-2", 10);
        mvc.perform(get("/api/v1/ecoimpacto").param("instituicaoId", instituicao.getId().toString())
                .header("Authorization", "Bearer " + tokenDono)).andExpect(status().isOk())
                .andExpect(jsonPath("impacto.co2Kg").value(200)).andExpect(jsonPath("fatoresUtilizados[0].versao").value("teste-1"));
    }

    @Test
    void litrosNaoSaoSomadosComQuilosEFatoresAusentesSaoInformados() throws Exception {
        long papel = cadastrar("PAPEL", "KG", 10);
        long oleo = cadastrar("OLEO", "L", 20);
        for (long id : new long[]{papel, oleo}) {
            alterar(id, "TRIAGEM", tokenAdmin, 200);
            alterar(id, "COLETADO", tokenAdmin, 200);
        }
        mvc.perform(get("/api/v1/materiais/resumo").param("instituicaoId", instituicao.getId().toString())
                .header("Authorization", "Bearer " + tokenDono)).andExpect(status().isOk())
                .andExpect(jsonPath("quantidades.KG").value(10)).andExpect(jsonPath("quantidades.L").value(20))
                .andExpect(jsonPath("registrosSemFator").value(2)).andExpect(jsonPath("impacto.co2Kg").value(0));
        var pontos = movimentos.findByInstituicaoIdOrderByDataCriacaoDesc(instituicao.getId());
        assertThat(pontos.stream().mapToInt(MovimentoScore::getFrequencia).sum()).isEqualTo(10);
    }

    @Test
    void acessoAIndicadoresPrivadosEConfiguracaoDeFatoresExigePermissao() throws Exception {
        String intruso = tokens.gerarToken(usuario(PerfilUsuario.OPERADOR));
        for (String endpoint : new String[]{"/api/v1/materiais/resumo", "/api/v1/ecoscore", "/api/v1/ecoimpacto", "/api/v1/dashboard"}) {
            mvc.perform(get(endpoint).param("instituicaoId", instituicao.getId().toString())
                    .header("Authorization", "Bearer " + intruso)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/ecoimpacto/fatores").header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dadosFator("teste", 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void coletaConcluidaCriaUmUnicoRegistroComQuantidadeReal() {
        Coleta coleta = coletas.saveAndFlush(Coleta.builder().usuarioSolicitante(dono).instituicao(instituicao)
                .tipoMaterial(TipoMaterial.METAL).quantidadeEstimadaKg(new BigDecimal("200"))
                .quantidadeReal(new BigDecimal("125.5")).unidade(UnidadeMaterial.KG).status(StatusColeta.REALIZADA)
                .dataConclusao(LocalDateTime.now()).dataHoraAgendamento(LocalDateTime.now().minusDays(1))
                .logradouro("Rua Um").numero("1").bairro("Centro").cidade("Sao Paulo").estado("SP").cep("01001000").build());
        RegistroMaterial primeiro = materialServico.registrarColetaConcluida(coleta, admin);
        RegistroMaterial segundo = materialServico.registrarColetaConcluida(coleta, admin);
        assertThat(segundo.getId()).isEqualTo(primeiro.getId());
        assertThat(primeiro.getQuantidade()).isEqualByComparingTo("125.5");
        assertThat(movimentos.findByInstituicaoIdOrderByDataCriacaoDesc(instituicao.getId())).hasSize(1);
    }

    @Test
    void rejeitaUnidadeIncompativelEPeriodoInvertido() throws Exception {
        mvc.perform(post("/api/v1/materiais").header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dadosMaterial("PAPEL", "L", 10))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/ecoimpacto").header("Authorization", "Bearer " + tokenDono)
                .param("inicio", "2026-02-01").param("fim", "2026-01-01")).andExpect(status().isBadRequest());
    }

    @Test
    void reprocessamentoPreservaFatoresVinculadosERelatoriosAnteriores() throws Exception {
        long id = cadastrar("PAPEL", "KG", 40);
        alterar(id, "TRIAGEM", tokenAdmin, 200);
        alterar(id, "COLETADO", tokenAdmin, 200);
        String gerado = mvc.perform(post("/api/v1/relatorios").header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("nome", "Antes do fator", "tipo", "IMPACTO",
                        "inicio", LocalDate.now().toString(), "fim", LocalDate.now().toString(), "instituicaoId", instituicao.getId()))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long relatorio = mapper.readTree(gerado).path("id").asLong();
        fator("primeira", 2);
        mvc.perform(post("/api/v1/ecoimpacto/reprocessar").header("Authorization", "Bearer " + tokenDono)
                .param("instituicaoId", instituicao.getId().toString()).param("inicio", LocalDate.now().toString()).param("fim", LocalDate.now().toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/ecoimpacto/reprocessar").header("Authorization", "Bearer " + tokenAdmin)
                .param("instituicaoId", instituicao.getId().toString()).param("inicio", LocalDate.now().toString()).param("fim", LocalDate.now().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("fatoresVinculados").value(1));
        fator("segunda", 10);
        mvc.perform(post("/api/v1/ecoimpacto/reprocessar").header("Authorization", "Bearer " + tokenAdmin)
                .param("instituicaoId", instituicao.getId().toString()).param("inicio", LocalDate.now().toString()).param("fim", LocalDate.now().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("fatoresVinculados").value(0));
        mvc.perform(get("/api/v1/ecoimpacto/ranking").header("Authorization", "Bearer " + tokenDono)
                .param("inicio", LocalDate.now().toString()).param("fim", LocalDate.now().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].impacto.co2Kg").value(80));
        mvc.perform(get("/api/v1/relatorios/" + relatorio).header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isOk()).andExpect(jsonPath("dados.resumo.impacto.co2Kg").value(0));
        mvc.perform(get("/api/v1/ecoselo/resumo").header("Authorization", "Bearer " + tokenDono))
                .andExpect(status().isOk()).andExpect(jsonPath("instituicoesCertificadas").value(0));
    }

    private Usuario usuario(PerfilUsuario perfil) {
        return usuarios.saveAndFlush(Usuario.builder().nome("Pessoa ambiental").email(UUID.randomUUID() + "@example.test")
                .senha("hash-teste").perfil(perfil).status(StatusUsuario.ATIVO).build());
    }

    private Map<String, Object> dadosMaterial(String material, String unidade, int quantidade) {
        return Map.of("material", material, "unidade", unidade, "quantidade", quantidade,
                "origem", "Origem teste", "destino", "Destino teste", "data", LocalDate.now().toString(), "instituicaoId", instituicao.getId());
    }

    private long cadastrar(String material, String unidade, int quantidade) throws Exception {
        String resposta = mvc.perform(post("/api/v1/materiais").header("Authorization", "Bearer " + tokenDono)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dadosMaterial(material, unidade, quantidade))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(resposta).get("id").asLong();
    }

    private void alterar(long id, String novoStatus, String token, int esperado) throws Exception {
        mvc.perform(patch("/api/v1/materiais/" + id + "/status").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("status", novoStatus))))
                .andExpect(status().is(esperado));
    }

    private Map<String, Object> dadosFator(String versao, int co2) {
        return Map.of("material", "PAPEL", "unidade", "KG", "versao", versao,
                "fonte", "Coeficientes sinteticos exclusivos do teste", "vigencia", LocalDate.now().toString(),
                "co2Kg", co2, "aguaL", 3, "energiaKwh", 4, "arvores", new BigDecimal("0.02"));
    }

    private void fator(String versao, int co2) throws Exception {
        mvc.perform(post("/api/v1/ecoimpacto/fatores").header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(dadosFator(versao, co2))))
                .andExpect(status().isCreated());
    }
}
