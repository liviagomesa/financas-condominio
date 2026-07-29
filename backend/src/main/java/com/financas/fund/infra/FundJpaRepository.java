package com.financas.fund.infra;

import com.financas.fund.domain.Fund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundJpaRepository extends JpaRepository<Fund, Long> {

    @Query("SELECT f FROM Fund f WHERE LOWER(TRIM(f.name)) = LOWER(TRIM(:name))")
    Optional<Fund> findByNormalizedName(@Param("name") String name);

    List<Fund> findAllByOrderByNameAsc();
}
