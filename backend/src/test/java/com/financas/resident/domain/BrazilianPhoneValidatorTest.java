package com.financas.resident.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BrazilianPhoneValidatorTest {

    private final BrazilianPhoneValidator validator = new BrazilianPhoneValidator();

    @ParameterizedTest
    @ValueSource(strings = {"11987654321", "1123456789", "(11) 91234-5678", "(11) 1234-5678"})
    void acceptsValidBrazilianPhoneFormats(String phone) {
        assertThat(validator.isValid(phone, null)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void acceptsBlankOrNullBecausePhoneIsOptional(String phone) {
        assertThat(validator.isValid(phone, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "119876543210", "abcdefghij", "119876543"})
    void rejectsInvalidBrazilianPhoneFormats(String phone) {
        assertThat(validator.isValid(phone, null)).isFalse();
    }
}
