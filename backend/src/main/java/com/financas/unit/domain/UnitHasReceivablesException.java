package com.financas.unit.domain;

import com.financas.shared.exceptions.ConflictException;

public class UnitHasReceivablesException extends ConflictException {

    public UnitHasReceivablesException() {
        super("Esta unidade possui lançamentos de contas a receber vinculados e não pode ser removida.");
    }
}
