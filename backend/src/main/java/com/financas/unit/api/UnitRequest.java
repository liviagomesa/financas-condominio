package com.financas.unit.api;

import jakarta.validation.constraints.NotBlank;

public record UnitRequest(
        @NotBlank(message = "O identificador da unidade é obrigatório.") String identifier) {
}
