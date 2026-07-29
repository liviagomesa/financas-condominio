package com.financas.group.infra;

import com.financas.group.domain.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupJpaRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM PartyGroup g WHERE LOWER(TRIM(g.name)) = LOWER(TRIM(:name))")
    Optional<Group> findByNormalizedName(@Param("name") String name);

    List<Group> findAllByOrderByNameAsc();
}
