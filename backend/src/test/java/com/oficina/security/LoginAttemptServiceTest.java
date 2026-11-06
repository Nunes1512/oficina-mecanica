package com.oficina.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    private MutableClock relogio;
    private LoginAttemptService servico;

    @BeforeEach
    void preparar() {
        relogio = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
        servico = new LoginAttemptService(3, Duration.ofMinutes(15), relogio);
    }

    @Test
    void naoBloqueiaAntesDoLimite() {
        servico.falha("ana@oficina.com");
        servico.falha("ana@oficina.com");
        assertThat(servico.bloqueado("ana@oficina.com")).isFalse();
    }

    @Test
    void bloqueiaNoLimite() {
        for (int i = 0; i < 3; i++) {
            servico.falha("ana@oficina.com");
        }
        assertThat(servico.bloqueado("ana@oficina.com")).isTrue();
    }

    @Test
    void liberaDepoisDoTempoDeBloqueio() {
        for (int i = 0; i < 3; i++) {
            servico.falha("ana@oficina.com");
        }
        relogio.avancar(Duration.ofMinutes(16));
        assertThat(servico.bloqueado("ana@oficina.com")).isFalse();
    }

    @Test
    void sucessoZeraOContador() {
        servico.falha("ana@oficina.com");
        servico.falha("ana@oficina.com");
        servico.sucesso("ana@oficina.com");
        servico.falha("ana@oficina.com");
        assertThat(servico.bloqueado("ana@oficina.com")).isFalse();
    }

    @Test
    void ignoraMaiusculasEEspacos() {
        servico.falha("ANA@oficina.com");
        servico.falha(" ana@oficina.com ");
        servico.falha("Ana@Oficina.com");
        assertThat(servico.bloqueado("ana@oficina.com")).isTrue();
    }

    @Test
    void contaPorUsuarioSeparadamente() {
        for (int i = 0; i < 3; i++) {
            servico.falha("ana@oficina.com");
        }
        assertThat(servico.bloqueado("bia@oficina.com")).isFalse();
    }

    /** Relógio que o teste controla, para simular a passagem do tempo sem esperar. */
    private static class MutableClock extends Clock {
        private Instant agora;

        MutableClock(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
