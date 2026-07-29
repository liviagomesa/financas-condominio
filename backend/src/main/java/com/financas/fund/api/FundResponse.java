package com.financas.fund.api;

import com.financas.fund.domain.Fund;
import java.math.BigDecimal;

public record FundResponse(Long id, String name, BigDecimal initialBalance, BigDecimal realBalance) {

    public static FundResponse from(Fund fund, BigDecimal realBalance) {
        return new FundResponse(fund.getId(), fund.getName(), fund.getInitialBalance(), realBalance);
    }
}
