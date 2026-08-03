package com.financas.recurringcharge.infra;

import com.financas.recurringcharge.domain.RecurringCharge;
import com.financas.recurringcharge.domain.RecurringChargeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RecurringChargeRepositoryImpl implements RecurringChargeRepository {

    private final RecurringChargeJpaRepository jpaRepository;

    public RecurringChargeRepositoryImpl(RecurringChargeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RecurringCharge save(RecurringCharge recurringCharge) {
        return jpaRepository.save(recurringCharge);
    }

    @Override
    public Optional<RecurringCharge> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<RecurringCharge> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsByPartyIdAndDeactivatedAtIsNull(Long partyId) {
        return jpaRepository.existsByPartyIdAndDeactivatedAtIsNull(partyId);
    }

    @Override
    public boolean existsByFundIdAndDeactivatedAtIsNull(Long fundId) {
        return jpaRepository.existsByFundIdAndDeactivatedAtIsNull(fundId);
    }
}
