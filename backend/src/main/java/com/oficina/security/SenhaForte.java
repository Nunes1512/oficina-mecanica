package com.oficina.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida a senha pela política do sistema. Campo vazio é tratado pelo @NotBlank. */
@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {

    String message() default PasswordPolicy.MENSAGEM;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
