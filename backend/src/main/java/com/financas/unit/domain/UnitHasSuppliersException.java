package com.financas.unit.domain;

import com.financas.shared.exceptions.ConflictException;

public class UnitHasSuppliersException extends ConflictException {

    public UnitHasSuppliersException() {
        super("Esta unidade possui fornecedores vinculados e não pode ser removida.");
    }
}
