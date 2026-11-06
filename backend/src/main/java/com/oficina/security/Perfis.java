package com.oficina.security;

/** Nomes dos perfis, exatamente como estão na tabela funcao (com o prefixo ROLE_ do Spring Security). */
public final class Perfis {

    public static final String ADMIN_MASTER = "ROLE_ADMIN_MASTER";
    public static final String GERENTE = "ROLE_GERENTE";
    public static final String FUNCIONARIO = "ROLE_FUNCIONARIO";

    private Perfis() {
    }
}
