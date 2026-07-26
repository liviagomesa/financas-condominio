package com.financas.unit.domain;

import com.financas.shared.exceptions.ConflictException;

public class UnitHasResidentsException extends ConflictException {

    public UnitHasResidentsException() {
        super("Esta unidade possui condôminos vinculados e não pode ser removida.");
    }
}
