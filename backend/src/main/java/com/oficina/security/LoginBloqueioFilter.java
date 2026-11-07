package com.oficina.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Barra a tentativa de login de quem está bloqueado por excesso de erros, antes de conferir a senha.
 * Vale para qualquer e-mail digitado, exista ou não no sistema, então a mensagem não revela quem tem conta.
 * Também impede que um atacante continue testando senhas durante o bloqueio.
 */
public class LoginBloqueioFilter extends OncePerRequestFilter {

    private final LoginAttemptService tentativas;

    public LoginBloqueioFilter(LoginAttemptService tentativas) {
        this.tentativas = tentativas;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean envioDoLogin = "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().equals(request.getContextPath() + "/login");
        return !envioDoLogin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (tentativas.bloqueado(request.getParameter("username"))) {
            response.sendRedirect(request.getContextPath() + "/login?bloqueado");
            return;
        }
        chain.doFilter(request, response);
    }
}
