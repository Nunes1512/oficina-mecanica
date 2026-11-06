package com.oficina.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Alimenta o bloqueio por tentativas com os eventos de login do Spring Security. */
@Component
@RequiredArgsConstructor
public class AutenticacaoEventosListener {

    private final LoginAttemptService tentativas;

    @EventListener
    public void aoFalhar(AuthenticationFailureBadCredentialsEvent evento) {
        tentativas.falha(evento.getAuthentication().getName());
    }

    @EventListener
    public void aoAutenticar(AuthenticationSuccessEvent evento) {
        tentativas.sucesso(evento.getAuthentication().getName());
    }
}
