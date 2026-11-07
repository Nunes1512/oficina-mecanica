package com.oficina.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Decide a mensagem depois de um login recusado. Se esta falha foi a que estourou o limite de tentativas,
 * já mostra o aviso de bloqueio, em vez de esperar a próxima tentativa.
 */
public class LoginFalhaHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService tentativas;

    public LoginFalhaHandler(LoginAttemptService tentativas) {
        this.tentativas = tentativas;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String destino = tentativas.bloqueado(request.getParameter("username")) ? "/login?bloqueado" : "/login?erro";
        response.sendRedirect(request.getContextPath() + destino);
    }
}
