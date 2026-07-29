package com.financas.account.domain;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(Long id);

    List<Account> findAll();

    List<Account> findByUnitId(Long unitId);

    List<Account> findBySupplierId(Long supplierId);

    List<Account> findByFundId(Long fundId);

    void deleteById(Long id);

    boolean existsById(Long id);

    boolean existsByUnitId(Long unitId);

    boolean existsBySupplierId(Long supplierId);

    boolean existsByFundId(Long fundId);
}
