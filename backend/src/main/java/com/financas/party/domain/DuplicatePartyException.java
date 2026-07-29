package com.financas.party.domain;

import com.financas.shared.exceptions.ConflictException;

public class DuplicatePartyException extends ConflictException {

    public DuplicatePartyException(String name) {
        super("Já existe uma parte cadastrada com o nome '" + name + "'.");
    }
}
