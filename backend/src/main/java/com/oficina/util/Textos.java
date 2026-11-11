package com.oficina.util;

import com.oficina.exception.NegocioException;
import java.util.Locale;

/** Normalização de dados; CPF verifica os dois dígitos, além do formato. */
public final class Textos {
    private Textos() { }

    public static String limpar(String texto) {
        if (texto == null) return null;
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    public static String cpf(String texto) {
        String digitos = texto == null ? "" : texto.replaceAll("\\D", "");
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1
                || digitoCpf(digitos, 9) != digitos.charAt(9) - '0'
                || digitoCpf(digitos, 10) != digitos.charAt(10) - '0') {
            throw new NegocioException("CPF inválido: confira os dígitos verificadores.");
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    private static int digitoCpf(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static String placa(String texto) {
        return texto == null ? null : texto.trim().toUpperCase(Locale.ROOT);
    }
}
