package com.financas.account.infra;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountRepository;
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
    public List<Account> findByUnitId(Long unitId) {
        return jpaRepository.findByUnitId(unitId);
    }

    @Override
    public List<Account> findBySupplierId(Long supplierId) {
        return jpaRepository.findBySupplierId(supplierId);
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
    public boolean existsByUnitId(Long unitId) {
        return jpaRepository.existsByUnitId(unitId);
    }

    @Override
    public boolean existsBySupplierId(Long supplierId) {
        return jpaRepository.existsBySupplierId(supplierId);
    }
}
