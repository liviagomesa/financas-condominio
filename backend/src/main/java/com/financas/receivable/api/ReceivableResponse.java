package com.financas.receivable.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.financas.receivable.domain.Receivable;
import com.financas.receivable.domain.TargetAccount;
import com.financas.unit.api.UnitResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableResponse(
        Long id,
        BigDecimal amount,
        @JsonFormat(pattern = "dd/MM/yyyy") LocalDate dueDate,
        String description,
        TargetAccount targetAccount,
        boolean recurring,
        UnitResponse unit) {

    public static ReceivableResponse from(Receivable receivable) {
        return new ReceivableResponse(
                receivable.getId(),
                receivable.getAmount(),
                receivable.getDueDate(),
                receivable.getDescription(),
                receivable.getTargetAccount(),
                receivable.isRecurring(),
                UnitResponse.from(receivable.getUnit()));
    }
}
