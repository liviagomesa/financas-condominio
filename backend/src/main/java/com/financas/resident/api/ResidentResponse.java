package com.financas.resident.api;

import com.financas.resident.domain.Resident;
import com.financas.unit.api.UnitResponse;

public record ResidentResponse(Long id, String name, UnitResponse unit, String email, String phone) {

    public static ResidentResponse from(Resident resident) {
        return new ResidentResponse(
                resident.getId(),
                resident.getName(),
                UnitResponse.from(resident.getUnit()),
                resident.getEmail(),
                resident.getPhone());
    }
}
