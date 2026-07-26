package com.financas.receivable.domain;

import java.util.List;
import java.util.Optional;

public interface ReceivableRepository {

    Receivable save(Receivable receivable);

    Optional<Receivable> findById(Long id);

    List<Receivable> findAll();

    List<Receivable> findByUnitId(Long unitId);

    void deleteById(Long id);

    boolean existsById(Long id);

    boolean existsByUnitId(Long unitId);
}
