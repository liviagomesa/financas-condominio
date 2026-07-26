package com.financas.resident.infra;

import com.financas.resident.domain.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentJpaRepository extends JpaRepository<Resident, Long> {

    boolean existsByUnitId(Long unitId);
}
