package com.financas.account.domain;

import com.financas.shared.exceptions.ConflictException;

public class NoUnitsRegisteredException extends ConflictException {

    public NoUnitsRegisteredException() {
        super("Não há unidades cadastradas. Cadastre uma unidade antes de lançar contas a receber.");
    }
}
