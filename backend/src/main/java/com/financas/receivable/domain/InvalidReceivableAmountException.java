package com.financas.receivable.domain;

import com.financas.shared.exceptions.BadRequestException;

public class InvalidReceivableAmountException extends BadRequestException {

    public InvalidReceivableAmountException() {
        super("O valor do lançamento deve ser maior que zero.");
    }
}
