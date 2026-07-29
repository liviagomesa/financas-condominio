package com.financas.account.api;

import com.financas.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountBulkRequest(
        @NotNull(message = "O tipo da conta é obrigatório.") AccountType type,
        @NotNull(message = "O valor da conta é obrigatório.")
                @PositiveOrZero(message = "O valor da conta não pode ser negativo.")
                BigDecimal amount,
        @NotNull(message = "A data de vencimento é obrigatória.") LocalDate dueDate,
        @NotBlank(message = "A descrição da conta é obrigatória.") String description,
        @NotNull(message = "O fundo é obrigatório.") Long fundId,
        @NotNull(message = "O tipo do lançamento é obrigatório.") Boolean recurring,
        @NotNull(message = "O grupo é obrigatório.") Long groupId,
        LocalDate paymentDate,
        String observations) {
}
