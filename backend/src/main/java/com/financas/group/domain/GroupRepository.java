package com.financas.group.domain;

import java.util.List;
import java.util.Optional;

public interface GroupRepository {

    Group save(Group group);

    Optional<Group> findById(Long id);

    List<Group> findAll();

    Optional<Group> findByNormalizedName(String name);

    void deleteById(Long id);

    boolean existsById(Long id);
}
