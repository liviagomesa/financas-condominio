package com.financas.recurringcharge.domain;

import com.financas.shared.exceptions.BadRequestException;

public class RecurringChargeTypeChangeNotAllowedException extends BadRequestException {

    public RecurringChargeTypeChangeNotAllowedException() {
        super("Não é possível alterar o tipo de uma cobrança recorrente já criada.");
    }
}
