package com.oficina.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LoginBloqueioFilterTest {

    private LoginAttemptService tentativasComAnaBloqueada() {
        LoginAttemptService tentativas = new LoginAttemptService(2, Duration.ofMinutes(15), Clock.systemUTC());
        tentativas.falha("ana@oficina.com");
        tentativas.falha("ana@oficina.com");
        return tentativas;
    }

    private MockHttpServletRequest login(String metodo, String email) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest(metodo, "/login");
        requisicao.setParameter("username", email);
        return requisicao;
    }

    @Test
    void barraQuemEstaBloqueadoSemChegarNaSenha() throws Exception {
        LoginBloqueioFilter filtro = new LoginBloqueioFilter(tentativasComAnaBloqueada());
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain cadeia = new MockFilterChain();

        filtro.doFilter(login("POST", "ana@oficina.com"), resposta, cadeia);

        assertThat(resposta.getRedirectedUrl()).isEqualTo("/login?bloqueado");
        assertThat(cadeia.getRequest()).isNull();
    }

    @Test
    void bloqueioVaiParaQualquerEmailBloqueadoExistaOuNao() throws Exception {
        LoginAttemptService tentativas = new LoginAttemptService(2, Duration.ofMinutes(15), Clock.systemUTC());
        tentativas.falha("nao-existe@x.com");
        tentativas.falha("nao-existe@x.com");
        LoginBloqueioFilter filtro = new LoginBloqueioFilter(tentativas);
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(login("POST", "nao-existe@x.com"), resposta, new MockFilterChain());

        assertThat(resposta.getRedirectedUrl()).isEqualTo("/login?bloqueado");
    }

    @Test
    void deixaPassarQuemNaoEstaBloqueado() throws Exception {
        LoginBloqueioFilter filtro = new LoginBloqueioFilter(tentativasComAnaBloqueada());
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain cadeia = new MockFilterChain();

        filtro.doFilter(login("POST", "bia@oficina.com"), resposta, cadeia);

        assertThat(resposta.getRedirectedUrl()).isNull();
        assertThat(cadeia.getRequest()).isNotNull();
    }

    @Test
    void ignoraRequisicoesQueNaoSaoOEnvioDoLogin() throws Exception {
        LoginBloqueioFilter filtro = new LoginBloqueioFilter(tentativasComAnaBloqueada());
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain cadeia = new MockFilterChain();

        filtro.doFilter(login("GET", "ana@oficina.com"), resposta, cadeia);

        assertThat(cadeia.getRequest()).isNotNull();
    }
}
