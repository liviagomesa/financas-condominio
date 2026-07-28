package com.financas.supplier.domain;

import com.financas.account.domain.AccountRepository;
import com.financas.shared.exceptions.ConflictException;
import com.financas.shared.exceptions.NotFoundException;
import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SupplierService {

    private final SupplierRepository repository;
    private final UnitRepository unitRepository;
    private final AccountRepository accountRepository;

    public SupplierService(
            SupplierRepository repository, UnitRepository unitRepository, AccountRepository accountRepository) {
        this.repository = repository;
        this.unitRepository = unitRepository;
        this.accountRepository = accountRepository;
    }

    public Supplier create(String name, Long unitId, String pixKey) {
        Unit unit = findUnitOrThrow(unitId);
        return repository.save(new Supplier(name, unit, pixKey));
    }

    public List<Supplier> findAll() {
        return repository.findAll();
    }

    public Supplier findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Fornecedor não encontrado."));
    }

    public Supplier update(Long id, String name, Long unitId, String pixKey) {
        Supplier supplier = findById(id);
        Unit unit = findUnitOrThrow(unitId);
        supplier.setName(name);
        supplier.setUnit(unit);
        supplier.setPixKey(pixKey);
        return repository.save(supplier);
    }

    public void delete(Long id) {
        findById(id);
        if (accountRepository.existsBySupplierId(id)) {
            throw new ConflictException("Este fornecedor possui contas a pagar vinculadas e não pode ser removido.");
        }
        repository.deleteById(id);
    }

    private Unit findUnitOrThrow(Long unitId) {
        if (unitId == null) {
            return null;
        }
        return unitRepository.findById(unitId).orElseThrow(() -> new NotFoundException("Unidade não encontrada."));
    }
}
