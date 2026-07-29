package com.financas.party.api;

import jakarta.validation.constraints.NotBlank;

public record PartyRequest(
        @NotBlank(message = "O nome da parte é obrigatório.") String name, String pixKey) {
}
