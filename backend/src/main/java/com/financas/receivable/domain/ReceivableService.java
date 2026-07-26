package com.financas.receivable.domain;

import com.financas.shared.exceptions.NotFoundException;
import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReceivableService {

    private final ReceivableRepository repository;
    private final UnitRepository unitRepository;

    public ReceivableService(ReceivableRepository repository, UnitRepository unitRepository) {
        this.repository = repository;
        this.unitRepository = unitRepository;
    }

    public Receivable create(
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            TargetAccount targetAccount,
            boolean recurring,
            Long unitId) {
        validatePositiveAmount(amount);
        Unit unit = findUnitOrThrow(unitId);
        return repository.save(
                new Receivable(amount, dueDate, description, targetAccount, recurring, unit));
    }

    public List<Receivable> createForAllUnits(
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            TargetAccount targetAccount,
            boolean recurring) {
        validatePositiveAmount(amount);
        List<Unit> units = unitRepository.findAll();
        if (units.isEmpty()) {
            throw new NoUnitsRegisteredException();
        }
        return units.stream()
                .map(unit -> repository.save(
                        new Receivable(amount, dueDate, description, targetAccount, recurring, unit)))
                .toList();
    }

    public List<Receivable> findAll(Long unitId) {
        if (unitId == null) {
            return repository.findAll();
        }
        findUnitOrThrow(unitId);
        return repository.findByUnitId(unitId);
    }

    public Receivable findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lançamento não encontrado."));
    }

    public Receivable update(
            Long id,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            TargetAccount targetAccount,
            boolean recurring,
            Long unitId) {
        validatePositiveAmount(amount);
        Receivable receivable = findById(id);
        Unit unit = findUnitOrThrow(unitId);
        receivable.setAmount(amount);
        receivable.setDueDate(dueDate);
        receivable.setDescription(description);
        receivable.setTargetAccount(targetAccount);
        receivable.setRecurring(recurring);
        receivable.setUnit(unit);
        return repository.save(receivable);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Lançamento não encontrado.");
        }
        repository.deleteById(id);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReceivableAmountException();
        }
    }

    private Unit findUnitOrThrow(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new NotFoundException(
                        "Unidade não encontrada. Cadastre uma unidade antes de lançar uma conta a receber."));
    }
}
