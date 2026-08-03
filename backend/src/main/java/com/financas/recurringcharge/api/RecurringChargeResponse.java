package com.financas.recurringcharge.api;

import com.financas.account.domain.AccountType;
import com.financas.fund.api.FundResponse;
import com.financas.party.api.PartyResponse;
import com.financas.recurringcharge.domain.RecurringCharge;
import java.math.BigDecimal;

public record RecurringChargeResponse(
        Long id,
        AccountType type,
        BigDecimal amount,
        Integer dueDay,
        String description,
        FundResponse fund,
        PartyResponse party,
        String observations,
        boolean lastGenerationFailed) {

    public static RecurringChargeResponse from(RecurringCharge recurringCharge, FundResponse fundResponse) {
        return new RecurringChargeResponse(
                recurringCharge.getId(),
                recurringCharge.getType(),
                recurringCharge.getAmount(),
                recurringCharge.getDueDay(),
                recurringCharge.getDescription(),
                fundResponse,
                PartyResponse.from(recurringCharge.getParty()),
                recurringCharge.getObservations(),
                recurringCharge.isLastGenerationFailed());
    }
}
