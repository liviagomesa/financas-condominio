package com.financas.account.domain;

import com.financas.shared.exceptions.ConflictException;

public class EmptyGroupException extends ConflictException {

    public EmptyGroupException() {
        super("O grupo selecionado não possui integrantes. Adicione integrantes ao grupo antes de lançar contas em lote.");
    }
}
