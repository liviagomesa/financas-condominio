package com.financas.unit.domain;

import com.financas.shared.exceptions.ConflictException;

public class UnitHasAccountsException extends ConflictException {

    public UnitHasAccountsException() {
        super("Esta unidade possui contas vinculadas e não pode ser removida.");
    }
}
