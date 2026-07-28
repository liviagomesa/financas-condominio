package com.financas.supplier.api;

import com.financas.supplier.domain.Supplier;
import com.financas.unit.api.UnitResponse;

public record SupplierResponse(Long id, String name, UnitResponse unit, String pixKey) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getUnit() != null ? UnitResponse.from(supplier.getUnit()) : null,
                supplier.getPixKey());
    }
}
