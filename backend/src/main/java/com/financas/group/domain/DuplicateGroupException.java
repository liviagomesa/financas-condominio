package com.financas.group.domain;

import com.financas.shared.exceptions.ConflictException;

public class DuplicateGroupException extends ConflictException {

    public DuplicateGroupException(String name) {
        super("Já existe um grupo cadastrado com o nome '" + name + "'.");
    }
}
