package com.financas.fund.domain;

import com.financas.shared.exceptions.ConflictException;

public class FundHasAccountsException extends ConflictException {

    public FundHasAccountsException() {
        super("Este fundo possui contas vinculadas e não pode ser removido.");
    }
}
