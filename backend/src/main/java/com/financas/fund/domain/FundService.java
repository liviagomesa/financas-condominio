package com.financas.fund.domain;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountRepository;
import com.financas.account.domain.AccountType;
import com.financas.recurringcharge.domain.RecurringChargeRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class FundService {

    private final FundRepository repository;
    private final AccountRepository accountRepository;
    private final RecurringChargeRepository recurringChargeRepository;

    public FundService(
            FundRepository repository,
            AccountRepository accountRepository,
            RecurringChargeRepository recurringChargeRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.recurringChargeRepository = recurringChargeRepository;
    }

    public Fund create(String name, BigDecimal initialBalance) {
        validateNotDuplicate(name, null);
        return repository.save(new Fund(name, initialBalance));
    }

    public List<Fund> findAll() {
        return repository.findAll();
    }

    public Fund findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Fundo não encontrado."));
    }

    public Fund update(Long id, String name, BigDecimal initialBalance) {
        Fund fund = findById(id);
        validateNotDuplicate(name, id);
        fund.setName(name);
        fund.setInitialBalance(initialBalance);
        return repository.save(fund);
    }

    public void delete(Long id) {
        findById(id);
        if (accountRepository.existsByFundId(id)) {
            throw new FundHasAccountsException();
        }
        if (recurringChargeRepository.existsByFundIdAndDeactivatedAtIsNull(id)) {
            throw new FundHasActiveRecurringChargesException();
        }
        repository.deleteById(id);
    }

    public BigDecimal calculateRealBalance(Fund fund) {
        List<Account> accounts = accountRepository.findByFundId(fund.getId());
        BigDecimal balance = fund.getInitialBalance();
        for (Account account : accounts) {
            if (!account.isPaid()) {
                continue;
            }
            if (account.getType() == AccountType.RECEIVABLE) {
                balance = balance.add(account.getAmount());
            } else if (account.getType() == AccountType.PAYABLE) {
                balance = balance.subtract(account.getAmount());
            }
        }
        return balance;
    }

    private void validateNotDuplicate(String name, Long currentId) {
        repository.findByNormalizedName(name)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new DuplicateFundException(name);
                });
    }
}
