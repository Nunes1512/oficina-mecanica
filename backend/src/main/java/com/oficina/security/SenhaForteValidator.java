package com.oficina.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.isBlank() || PasswordPolicy.forte(valor);
    }
}
