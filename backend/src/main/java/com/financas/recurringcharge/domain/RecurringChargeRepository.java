package com.financas.recurringcharge.domain;

import java.util.List;
import java.util.Optional;

public interface RecurringChargeRepository {

    RecurringCharge save(RecurringCharge recurringCharge);

    Optional<RecurringCharge> findById(Long id);

    List<RecurringCharge> findAll();

    boolean existsByPartyIdAndDeactivatedAtIsNull(Long partyId);

    boolean existsByFundIdAndDeactivatedAtIsNull(Long fundId);
}
