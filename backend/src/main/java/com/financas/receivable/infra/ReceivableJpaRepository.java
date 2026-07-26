package com.financas.receivable.infra;

import com.financas.receivable.domain.Receivable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableJpaRepository extends JpaRepository<Receivable, Long> {

    List<Receivable> findByUnitId(Long unitId);

    boolean existsByUnitId(Long unitId);
}
