package com.financas.account.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(Long id);

    List<Account> findAll();

    List<Account> findByPartyId(Long partyId);

    List<Account> findByFundId(Long fundId);

    void deleteById(Long id);

    boolean existsById(Long id);

    boolean existsByPartyId(Long partyId);

    boolean existsByFundId(Long fundId);

    boolean existsByRecurringChargeIdAndDueDateBetween(Long recurringChargeId, LocalDate start, LocalDate end);
}
