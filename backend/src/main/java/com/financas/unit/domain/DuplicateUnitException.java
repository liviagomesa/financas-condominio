package com.financas.unit.domain;

import com.financas.shared.exceptions.ConflictException;

public class DuplicateUnitException extends ConflictException {

    public DuplicateUnitException(String identifier) {
        super("Já existe uma unidade cadastrada com o identificador '" + identifier + "'.");
    }
}
