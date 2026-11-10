package com.oficina.security;

/** Autoridades persistidas em funcao.nome (ROLE_ prefix obrigatório no Spring Security). */
public final class Perfis {
    public static final String ADMIN_MASTER = "ROLE_ADMIN_MASTER";
    public static final String GERENTE = "ROLE_GERENTE";
    public static final String MECANICO = "ROLE_MECANICO";
    public static final String ASSISTENTE_ADMINISTRATIVO = "ROLE_ASSISTENTE_ADMINISTRATIVO";
    /** Mantido para nao desativar logins historicos da Parte 3. */
    public static final String FUNCIONARIO = "ROLE_FUNCIONARIO";

    private Perfis() { }

    public static String rotulo(String perfil) {
        if (ADMIN_MASTER.equals(perfil)) return "Administrador Master";
        if (GERENTE.equals(perfil)) return "Gerente";
        if (MECANICO.equals(perfil)) return "Mecânico";
        if (ASSISTENTE_ADMINISTRATIVO.equals(perfil)) return "Assistente Administrativo";
        if (FUNCIONARIO.equals(perfil)) return "Funcionário (perfil antigo)";
        return perfil;
    }
}
