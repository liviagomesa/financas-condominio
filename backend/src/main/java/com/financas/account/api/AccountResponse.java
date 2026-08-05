package com.financas.account.api;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountType;
import com.financas.fund.api.FundSummaryResponse;
import com.financas.party.api.PartyResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountResponse(
        Long id,
        AccountType type,
        BigDecimal amount,
        LocalDate dueDate,
        String description,
        FundSummaryResponse fund,
        LocalDate paymentDate,
        String observations,
        PartyResponse party) {

    public static AccountResponse from(Account account, FundSummaryResponse fundResponse) {
        return new AccountResponse(
                account.getId(),
                account.getType(),
                account.getAmount(),
                account.getDueDate(),
                account.getDescription(),
                fundResponse,
                account.getPaymentDate(),
                account.getObservations(),
                PartyResponse.from(account.getParty()));
    }
}
