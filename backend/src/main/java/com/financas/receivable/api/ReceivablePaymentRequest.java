package com.financas.receivable.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReceivablePaymentRequest(
        @NotNull(message = "A data de pagamento é obrigatória.") LocalDate paymentDate) {
}
