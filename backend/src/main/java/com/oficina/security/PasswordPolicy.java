package com.oficina.security;

import java.util.regex.Pattern;

/** Regra única de senha forte, usada pelos formulários e pela criação do primeiro administrador. */
public final class PasswordPolicy {

    public static final String MENSAGEM =
            "A senha deve ter de 8 a 64 caracteres, com letra maiúscula, minúscula, número e símbolo.";

    private static final Pattern FORTE =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");

    private PasswordPolicy() {
    }

    public static boolean forte(String senha) {
        return senha != null && FORTE.matcher(senha).matches();
    }
}
