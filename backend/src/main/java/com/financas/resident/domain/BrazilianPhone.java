package com.financas.resident.domain;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrazilianPhoneValidator.class)
public @interface BrazilianPhone {

    String message() default "O telefone deve estar no formato brasileiro, com DDD (ex.: (11) 91234-5678).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
