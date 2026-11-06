package com.oficina.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Bloqueio temporário contra tentativas repetidas de senha (força bruta).
 * Depois de N falhas seguidas, o login fica bloqueado por alguns minutos. O estado fica em memória:
 * reiniciar a aplicação zera os contadores. O bloqueio permanente (coluna "bloqueado") é outra coisa.
 */
@Service
public class LoginAttemptService {

    private static final int LIMITE_DE_REGISTROS = 10_000;

    private final int maxTentativas;
    private final Duration duracaoBloqueio;
    private final Clock relogio;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(@Value("${app.security.max-login-attempts:5}") int maxTentativas,
                               @Value("${app.security.lock-minutes:15}") long minutosDeBloqueio) {
        this(maxTentativas, Duration.ofMinutes(minutosDeBloqueio), Clock.systemDefaultZone());
    }

    LoginAttemptService(int maxTentativas, Duration duracaoBloqueio, Clock relogio) {
        this.maxTentativas = maxTentativas;
        this.duracaoBloqueio = duracaoBloqueio;
        this.relogio = relogio;
    }

    public void falha(String login) {
        String chave = chave(login);
        if (chave == null) {
            return;
        }
        if (registros.size() >= LIMITE_DE_REGISTROS) {
            registros.clear();
        }
        Instant agora = relogio.instant();
        registros.compute(chave, (k, atual) -> {
            if (atual != null && atual.bloqueadoAte() != null && atual.bloqueadoAte().isAfter(agora)) {
                return atual;
            }
            boolean bloqueioExpirou = atual != null && atual.bloqueadoAte() != null;
            int tentativas = (atual == null || bloqueioExpirou ? 0 : atual.tentativas()) + 1;
            if (tentativas >= maxTentativas) {
                return new Registro(0, agora.plus(duracaoBloqueio));
            }
            return new Registro(tentativas, null);
        });
    }

    public void sucesso(String login) {
        liberar(login);
    }

    public void liberar(String login) {
        String chave = chave(login);
        if (chave != null) {
            registros.remove(chave);
        }
    }

    public boolean bloqueado(String login) {
        String chave = chave(login);
        if (chave == null) {
            return false;
        }
        Registro registro = registros.get(chave);
        return registro != null && registro.bloqueadoAte() != null
                && registro.bloqueadoAte().isAfter(relogio.instant());
    }

    private String chave(String login) {
        if (login == null || login.isBlank()) {
            return null;
        }
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private record Registro(int tentativas, Instant bloqueadoAte) {
    }
}
