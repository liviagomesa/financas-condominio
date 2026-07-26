package com.financas.unit.api;

import com.financas.unit.domain.Unit;

public record UnitResponse(Long id, String identifier) {

    public static UnitResponse from(Unit unit) {
        return new UnitResponse(unit.getId(), unit.getIdentifier());
    }
}
