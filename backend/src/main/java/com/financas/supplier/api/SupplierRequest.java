package com.financas.supplier.api;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
        @NotBlank(message = "O nome do fornecedor é obrigatório.") String name,
        Long unitId,
        String pixKey) {
}
