package com.financas.fund.domain;

import com.financas.shared.exceptions.ConflictException;

public class DuplicateFundException extends ConflictException {

    public DuplicateFundException(String name) {
        super("Já existe um fundo cadastrado com o nome '" + name + "'.");
    }
}
