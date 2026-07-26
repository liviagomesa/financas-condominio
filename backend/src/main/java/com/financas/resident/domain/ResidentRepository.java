package com.financas.resident.domain;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository {

    Resident save(Resident resident);

    Optional<Resident> findById(Long id);

    List<Resident> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);

    boolean existsByUnitId(Long unitId);
}
