package com.financas.resident.domain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BrazilianPhoneValidator implements ConstraintValidator<BrazilianPhone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.matches("\\d{10}|\\d{11}");
    }
}
