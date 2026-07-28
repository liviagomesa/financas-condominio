package com.financas.account.infra;

import com.financas.account.domain.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<Account, Long> {

    List<Account> findByUnitId(Long unitId);

    List<Account> findBySupplierId(Long supplierId);

    boolean existsByUnitId(Long unitId);

    boolean existsBySupplierId(Long supplierId);
}
