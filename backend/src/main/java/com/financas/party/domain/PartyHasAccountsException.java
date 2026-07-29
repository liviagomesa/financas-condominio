package com.financas.party.domain;

import com.financas.shared.exceptions.ConflictException;

public class PartyHasAccountsException extends ConflictException {

    public PartyHasAccountsException() {
        super("Esta parte possui contas vinculadas e não pode ser removida.");
    }
}
