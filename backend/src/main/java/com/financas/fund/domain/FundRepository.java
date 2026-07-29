package com.financas.fund.domain;

import java.util.List;
import java.util.Optional;

public interface FundRepository {

    Fund save(Fund fund);

    Optional<Fund> findById(Long id);

    List<Fund> findAll();

    Optional<Fund> findByNormalizedName(String name);

    void deleteById(Long id);

    boolean existsById(Long id);
}
