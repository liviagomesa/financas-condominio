package com.financas.unit.infra;

import com.financas.unit.domain.Unit;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnitJpaRepository extends JpaRepository<Unit, Long> {

    @Query("SELECT u FROM Unit u WHERE LOWER(TRIM(u.identifier)) = LOWER(TRIM(:identifier))")
    Optional<Unit> findByNormalizedIdentifier(@Param("identifier") String identifier);
}
