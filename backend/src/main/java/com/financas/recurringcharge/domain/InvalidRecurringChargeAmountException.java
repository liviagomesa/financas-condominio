package com.financas.recurringcharge.domain;

import com.financas.shared.exceptions.BadRequestException;

public class InvalidRecurringChargeAmountException extends BadRequestException {

    public InvalidRecurringChargeAmountException() {
        super("O valor da cobrança recorrente não pode ser negativo.");
    }
}
