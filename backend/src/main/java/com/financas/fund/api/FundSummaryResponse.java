package com.financas.fund.api;

import com.financas.fund.domain.Fund;
import java.math.BigDecimal;

public record FundSummaryResponse(Long id, String name, BigDecimal initialBalance) {

    public static FundSummaryResponse from(Fund fund) {
        return new FundSummaryResponse(fund.getId(), fund.getName(), fund.getInitialBalance());
    }
}
