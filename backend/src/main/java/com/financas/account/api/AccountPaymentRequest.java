package com.financas.account.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountPaymentRequest(
        @NotNull(message = "A data de pagamento é obrigatória.") LocalDate paymentDate,
        @Positive(message = "O valor pago deve ser maior que zero.") BigDecimal paidAmount) {
}
