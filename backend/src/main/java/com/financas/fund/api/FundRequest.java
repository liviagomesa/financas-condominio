package com.financas.fund.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FundRequest(
        @NotBlank(message = "O nome do fundo é obrigatório.") String name,
        @NotNull(message = "O saldo inicial é obrigatório.") BigDecimal initialBalance) {
}
