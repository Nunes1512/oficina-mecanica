package com.oficina.security;

/** Nomes dos perfis, exatamente como estão na tabela funcao (com o prefixo ROLE_ do Spring Security). */
public final class Perfis {

    public static final String ADMIN_MASTER = "ROLE_ADMIN_MASTER";
    public static final String GERENTE = "ROLE_GERENTE";
    public static final String FUNCIONARIO = "ROLE_FUNCIONARIO";

    private Perfis() {
    }

    /** Nome para mostrar na tela. O valor ROLE_* continua no banco porque o Spring Security exige o prefixo. */
    public static String rotulo(String perfil) {
        if (ADMIN_MASTER.equals(perfil)) {
            return "Administrador Master";
        }
        if (GERENTE.equals(perfil)) {
            return "Gerente";
        }
        if (FUNCIONARIO.equals(perfil)) {
            return "Funcionário";
        }
        return perfil;
    }
}
