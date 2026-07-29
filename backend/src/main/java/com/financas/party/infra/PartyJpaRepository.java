package com.financas.party.infra;

import com.financas.party.domain.Party;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyJpaRepository extends JpaRepository<Party, Long> {

    @Query("SELECT p FROM Party p WHERE LOWER(TRIM(p.name)) = LOWER(TRIM(:name))")
    Optional<Party> findByNormalizedName(@Param("name") String name);

    List<Party> findAllByOrderByNameAsc();
}
