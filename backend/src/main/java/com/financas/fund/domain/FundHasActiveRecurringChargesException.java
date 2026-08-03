package com.financas.fund.domain;

import com.financas.shared.exceptions.ConflictException;

public class FundHasActiveRecurringChargesException extends ConflictException {

    public FundHasActiveRecurringChargesException() {
        super("Este fundo possui cobranças recorrentes ativas e não pode ser removido.");
    }
}
