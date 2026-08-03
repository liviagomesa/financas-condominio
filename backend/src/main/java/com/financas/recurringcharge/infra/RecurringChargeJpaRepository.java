package com.financas.recurringcharge.infra;

import com.financas.recurringcharge.domain.RecurringCharge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringChargeJpaRepository extends JpaRepository<RecurringCharge, Long> {

    boolean existsByPartyIdAndDeactivatedAtIsNull(Long partyId);

    boolean existsByFundIdAndDeactivatedAtIsNull(Long fundId);
}
