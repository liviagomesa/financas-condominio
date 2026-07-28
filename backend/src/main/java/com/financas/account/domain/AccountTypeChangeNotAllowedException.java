package com.financas.account.domain;

import com.financas.shared.exceptions.BadRequestException;

public class AccountTypeChangeNotAllowedException extends BadRequestException {

    public AccountTypeChangeNotAllowedException() {
        super("Não é possível alterar o tipo de uma conta já criada.");
    }
}
