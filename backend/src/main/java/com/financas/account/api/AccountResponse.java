package com.financas.account.api;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountType;
import com.financas.fund.api.FundResponse;
import com.financas.party.api.PartyResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountResponse(
        Long id,
        AccountType type,
        BigDecimal amount,
        LocalDate dueDate,
        String description,
        FundResponse fund,
        boolean recurring,
        LocalDate paymentDate,
        String observations,
        PartyResponse party) {

    public static AccountResponse from(Account account, FundResponse fundResponse) {
        return new AccountResponse(
                account.getId(),
                account.getType(),
                account.getAmount(),
                account.getDueDate(),
                account.getDescription(),
                fundResponse,
                account.isRecurring(),
                account.getPaymentDate(),
                account.getObservations(),
                PartyResponse.from(account.getParty()));
    }
}
