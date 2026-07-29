package com.financas.group.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record GroupRequest(
        @NotBlank(message = "O nome do grupo é obrigatório.") String name, List<Long> partyIds) {
}
