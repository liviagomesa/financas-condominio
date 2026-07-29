package com.financas.party.domain;

import java.util.List;
import java.util.Optional;

public interface PartyRepository {

    Party save(Party party);

    Optional<Party> findById(Long id);

    List<Party> findAll();

    Optional<Party> findByNormalizedName(String name);

    void deleteById(Long id);

    boolean existsById(Long id);
}
