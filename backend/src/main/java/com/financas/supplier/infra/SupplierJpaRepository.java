package com.financas.supplier.infra;

import com.financas.supplier.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierJpaRepository extends JpaRepository<Supplier, Long> {

    boolean existsByUnitId(Long unitId);
}
