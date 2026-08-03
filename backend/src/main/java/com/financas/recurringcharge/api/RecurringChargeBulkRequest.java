package com.financas.recurringcharge.api;

import com.financas.account.domain.AccountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record RecurringChargeBulkRequest(
        @NotNull(message = "O tipo da cobrança recorrente é obrigatório.") AccountType type,
        @NotNull(message = "O valor da cobrança recorrente é obrigatório.")
                @PositiveOrZero(message = "O valor da cobrança recorrente não pode ser negativo.")
                BigDecimal amount,
        @NotNull(message = "O dia de vencimento é obrigatório.")
                @Min(value = 1, message = "O dia de vencimento deve estar entre 1 e 31.")
                @Max(value = 31, message = "O dia de vencimento deve estar entre 1 e 31.")
                Integer dueDay,
        @NotBlank(message = "A descrição da cobrança recorrente é obrigatória.") String description,
        @NotNull(message = "O fundo é obrigatório.") Long fundId,
        @NotNull(message = "O grupo é obrigatório.") Long groupId,
        String observations) {
}
