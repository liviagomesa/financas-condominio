package com.financas.resident.api;

import com.financas.resident.domain.BrazilianPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResidentRequest(
        @NotBlank(message = "O nome do condômino é obrigatório.") String name,
        @NotNull(message = "A unidade do condômino é obrigatória.") Long unitId,
        @Email(message = "Informe um e-mail em formato válido.") String email,
        @BrazilianPhone String phone) {
}
