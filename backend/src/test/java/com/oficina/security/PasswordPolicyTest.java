package com.oficina.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

    @Test
    void aceitaSenhaForte() {
        assertThat(PasswordPolicy.forte("Senha@123")).isTrue();
    }

    @Test
    void recusaSenhaNula() {
        assertThat(PasswordPolicy.forte(null)).isFalse();
    }

    @Test
    void recusaSenhaMuitoLonga() {
        assertThat(PasswordPolicy.forte("Aa1@" + "x".repeat(61))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"senha@123", "SENHA@123", "Senha@abc", "Senha1234", "Ab@1", ""})
    void recusaSenhaFraca(String senha) {
        assertThat(PasswordPolicy.forte(senha)).isFalse();
    }
}
