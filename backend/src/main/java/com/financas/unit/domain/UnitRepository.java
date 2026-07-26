package com.financas.unit.domain;

import java.util.List;
import java.util.Optional;

public interface UnitRepository {

    Unit save(Unit unit);

    Optional<Unit> findById(Long id);

    List<Unit> findAll();

    Optional<Unit> findByNormalizedIdentifier(String identifier);

    void deleteById(Long id);

    boolean existsById(Long id);
}
