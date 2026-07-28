package com.financas.unit.domain;

import com.financas.account.domain.AccountRepository;
import com.financas.shared.exceptions.NotFoundException;
import com.financas.supplier.domain.SupplierRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class UnitService {

    private final UnitRepository repository;
    private final AccountRepository accountRepository;
    private final SupplierRepository supplierRepository;

    public UnitService(
            UnitRepository repository, AccountRepository accountRepository, SupplierRepository supplierRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.supplierRepository = supplierRepository;
    }

    public Unit create(String identifier) {
        validateNotDuplicate(identifier, null);
        return repository.save(new Unit(identifier));
    }

    public List<Unit> findAll() {
        return repository.findAll();
    }

    public Unit findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unidade não encontrada."));
    }

    public Unit update(Long id, String identifier) {
        Unit unit = findById(id);
        validateNotDuplicate(identifier, id);
        unit.setIdentifier(identifier);
        return repository.save(unit);
    }

    public void delete(Long id) {
        findById(id);
        if (accountRepository.existsByUnitId(id)) {
            throw new UnitHasAccountsException();
        }
        if (supplierRepository.existsByUnitId(id)) {
            throw new UnitHasSuppliersException();
        }
        repository.deleteById(id);
    }

    private void validateNotDuplicate(String identifier, Long currentId) {
        repository.findByNormalizedIdentifier(identifier)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new DuplicateUnitException(identifier);
                });
    }
}
