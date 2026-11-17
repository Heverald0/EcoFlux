package com.example.EcoFlux;

import com.example.EcoFlux.configuracao.FiltroLimiteAcesso;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

class FiltroLimiteAcessoTest {
    @Test
    void limitaTentativasPorEnderecoSemConfiarEmCabecalhoForjado() throws Exception {
        FiltroLimiteAcesso filtro = new FiltroLimiteAcesso(new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(filtro, "limite", 2);
        ReflectionTestUtils.setField(filtro, "segundos", 60L);
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            MockHttpServletResponse resposta = executar(filtro, "192.0.2.1", "/api/v1/auth/login", "198.51.100." + tentativa);
            assertThat(resposta.getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse bloqueada = executar(filtro, "192.0.2.1", "/api/v1/auth/login", "203.0.113.2");
        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isEqualTo("60");
        assertThat(executar(filtro, "192.0.2.2", "/api/v1/auth/login", "").getStatus()).isEqualTo(200);
        assertThat(executar(filtro, "192.0.2.1", "/api/v1/coletas", "").getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse executar(FiltroLimiteAcesso filtro, String endereco, String caminho, String encaminhado) throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("POST", caminho);
        requisicao.setRemoteAddr(endereco);
        requisicao.addHeader("X-Forwarded-For", encaminhado);
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao, resposta, new MockFilterChain());
        return resposta;
    }
}
