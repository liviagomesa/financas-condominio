package com.financas.account.infra;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public AccountRepositoryImpl(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Account save(Account account) {
        return jpaRepository.save(account);
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Account> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Account> findByPartyId(Long partyId) {
        return jpaRepository.findByPartyId(partyId);
    }

    @Override
    public List<Account> findByFundId(Long fundId) {
        return jpaRepository.findByFundId(fundId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByPartyId(Long partyId) {
        return jpaRepository.existsByPartyId(partyId);
    }

    @Override
    public boolean existsByFundId(Long fundId) {
        return jpaRepository.existsByFundId(fundId);
    }

    @Override
    public boolean existsByRecurringChargeIdAndDueDateBetween(Long recurringChargeId, LocalDate start, LocalDate end) {
        return jpaRepository.existsByRecurringChargeIdAndDueDateBetween(recurringChargeId, start, end);
    }
}
