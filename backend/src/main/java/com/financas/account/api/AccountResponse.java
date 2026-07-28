package com.financas.account.api;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountType;
import com.financas.account.domain.Fund;
import com.financas.supplier.api.SupplierResponse;
import com.financas.unit.api.UnitResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountResponse(
        Long id,
        AccountType type,
        BigDecimal amount,
        LocalDate dueDate,
        String description,
        Fund fund,
        boolean recurring,
        LocalDate paymentDate,
        String observations,
        UnitResponse unit,
        SupplierResponse supplier) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getType(),
                account.getAmount(),
                account.getDueDate(),
                account.getDescription(),
                account.getFund(),
                account.isRecurring(),
                account.getPaymentDate(),
                account.getObservations(),
                account.getUnit() != null ? UnitResponse.from(account.getUnit()) : null,
                account.getSupplier() != null ? SupplierResponse.from(account.getSupplier()) : null);
    }
}
