package com.oficina.util;

import com.oficina.exception.NegocioException;
import java.util.Locale;

/** Normalização de textos digitados nos formulários. */
public final class Textos {

    private Textos() {
    }

    /** Remove espaços das pontas e troca texto vazio por null (assim o banco guarda NULL, não ""). */
    public static String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /** Aceita com ou sem pontuação e devolve sempre no formato 000.000.000-00, que é o do banco. */
    public static String cpf(String texto) {
        String numeros = texto == null ? "" : texto.replaceAll("\\D", "");
        if (numeros.length() != 11) {
            throw new NegocioException("CPF inválido. Use 000.000.000-00.");
        }
        return numeros.substring(0, 3) + "." + numeros.substring(3, 6) + "."
                + numeros.substring(6, 9) + "-" + numeros.substring(9);
    }

    public static String placa(String texto) {
        return texto == null ? null : texto.trim().toUpperCase(Locale.ROOT);
    }
}
