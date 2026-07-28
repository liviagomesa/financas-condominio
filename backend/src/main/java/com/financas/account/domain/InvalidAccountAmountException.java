package com.financas.account.domain;

import com.financas.shared.exceptions.BadRequestException;

public class InvalidAccountAmountException extends BadRequestException {

    public InvalidAccountAmountException() {
        super("O valor da conta não pode ser negativo.");
    }
}
