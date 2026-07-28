package com.financas.account.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AccountPaymentRequest(
        @NotNull(message = "A data de pagamento é obrigatória.") LocalDate paymentDate) {
}
