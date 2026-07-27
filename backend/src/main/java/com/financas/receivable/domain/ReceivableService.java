package com.financas.receivable.domain;

import com.financas.shared.exceptions.BadRequestException;
import com.financas.shared.exceptions.NotFoundException;
import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
            Long unitId,
            LocalDate paymentDate) {
        validatePositiveAmount(amount);
        Unit unit = findUnitOrThrow(unitId);
        return repository.save(
                new Receivable(amount, dueDate, description, targetAccount, recurring, unit, paymentDate));
    }

    public List<Receivable> createForAllUnits(
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            TargetAccount targetAccount,
            boolean recurring,
            LocalDate paymentDate) {
        validatePositiveAmount(amount);
        List<Unit> units = unitRepository.findAll();
        if (units.isEmpty()) {
            throw new NoUnitsRegisteredException();
        }
        return units.stream()
                .map(unit -> repository.save(new Receivable(
                        amount, dueDate, description, targetAccount, recurring, unit, paymentDate)))
                .toList();
    }

    /**
     * Dado o volume pequeno de registros (poucas dezenas), a filtragem é feita em memória, sem
     * necessidade de índices ou consultas otimizadas dedicadas.
     */
    public List<Receivable> findAll(
            Long unitId, Boolean paid, Boolean overdue, String dueYearMonth, String paymentYearMonth) {
        List<Receivable> receivables;
        if (unitId == null) {
            receivables = repository.findAll();
        } else {
            findUnitOrThrow(unitId);
            receivables = repository.findByUnitId(unitId);
        }

        if (paid != null) {
            receivables = receivables.stream().filter(r -> r.isPaid() == paid).toList();
        }
        if (Boolean.TRUE.equals(overdue)) {
            LocalDate today = LocalDate.now();
            receivables = receivables.stream()
                    .filter(r -> !r.isPaid() && r.getDueDate().isBefore(today))
                    .toList();
        }
        if (dueYearMonth != null) {
            YearMonth month = parseYearMonth(dueYearMonth, "vencimento");
            receivables = receivables.stream()
                    .filter(r -> YearMonth.from(r.getDueDate()).equals(month))
                    .toList();
        }
        if (paymentYearMonth != null) {
            YearMonth month = parseYearMonth(paymentYearMonth, "pagamento");
            receivables = receivables.stream()
                    .filter(r -> r.isPaid() && YearMonth.from(r.getPaymentDate()).equals(month))
                    .toList();
        }
        return receivables;
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
            Long unitId,
            LocalDate paymentDate) {
        validatePositiveAmount(amount);
        Receivable receivable = findById(id);
        Unit unit = findUnitOrThrow(unitId);
        receivable.setAmount(amount);
        receivable.setDueDate(dueDate);
        receivable.setDescription(description);
        receivable.setTargetAccount(targetAccount);
        receivable.setRecurring(recurring);
        receivable.setUnit(unit);
        receivable.setPaymentDate(paymentDate);
        return repository.save(receivable);
    }

    public Receivable registerPayment(Long id, LocalDate paymentDate) {
        Receivable receivable = findById(id);
        receivable.setPaymentDate(paymentDate);
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

    private YearMonth parseYearMonth(String value, String fieldLabel) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(
                    "Mês/ano de " + fieldLabel + " inválido. Use o formato AAAA-MM.");
        }
    }
}
