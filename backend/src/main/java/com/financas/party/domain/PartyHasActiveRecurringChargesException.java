package com.financas.party.domain;

import com.financas.shared.exceptions.ConflictException;

public class PartyHasActiveRecurringChargesException extends ConflictException {

    public PartyHasActiveRecurringChargesException() {
        super("Esta parte possui cobranças recorrentes ativas e não pode ser removida.");
    }
}
