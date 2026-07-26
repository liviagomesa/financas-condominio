package com.financas.receivable.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financas.receivable.domain.TargetAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableRequest(
        @NotNull(message = "O valor do lançamento é obrigatório.")
                @Positive(message = "O valor do lançamento deve ser maior que zero.")
                BigDecimal amount,
        @NotNull(message = "A data de vencimento é obrigatória.")
                @JsonFormat(pattern = "dd/MM/yyyy")
                LocalDate dueDate,
        @NotBlank(message = "A descrição do lançamento é obrigatória.") String description,
        @NotNull(message = "A conta destino é obrigatória.") TargetAccount targetAccount,
        @NotNull(message = "O tipo do lançamento é obrigatório.") Boolean recurring,
        @NotNull(message = "A unidade é obrigatória.") Long unitId) {
}
