package com.financas.account.domain;

import com.financas.shared.exceptions.BadRequestException;
import com.financas.shared.exceptions.NotFoundException;
import com.financas.supplier.domain.Supplier;
import com.financas.supplier.domain.SupplierRepository;
import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository repository;
    private final UnitRepository unitRepository;
    private final SupplierRepository supplierRepository;

    public AccountService(
            AccountRepository repository, UnitRepository unitRepository, SupplierRepository supplierRepository) {
        this.repository = repository;
        this.unitRepository = unitRepository;
        this.supplierRepository = supplierRepository;
    }

    public Account create(
            AccountType type,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Fund fund,
            boolean recurring,
            Long unitId,
            Long supplierId,
            LocalDate paymentDate,
            String observations) {
        validateNonNegativeAmount(amount);
        Unit unit = resolveUnit(type, unitId, supplierId);
        Supplier supplier = resolveSupplier(type, unitId, supplierId);
        return repository.save(new Account(
                type, amount, dueDate, description, fund, recurring, unit, supplier, paymentDate, observations));
    }

    public List<Account> createForAllUnits(
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Fund fund,
            boolean recurring,
            LocalDate paymentDate,
            String observations) {
        validateNonNegativeAmount(amount);
        List<Unit> units = unitRepository.findAll();
        if (units.isEmpty()) {
            throw new NoUnitsRegisteredException();
        }
        return units.stream()
                .map(unit -> repository.save(new Account(
                        AccountType.RECEIVABLE,
                        amount,
                        dueDate,
                        description,
                        fund,
                        recurring,
                        unit,
                        null,
                        paymentDate,
                        observations)))
                .toList();
    }

    /**
     * Dado o volume pequeno de registros (poucas dezenas), a filtragem é feita em memória, sem
     * necessidade de índices ou consultas otimizadas dedicadas.
     */
    public List<Account> findAll(
            Long unitId,
            Long supplierId,
            AccountType type,
            Boolean paid,
            Boolean overdue,
            String dueYearMonth,
            String paymentYearMonth) {
        List<Account> accounts;
        if (unitId != null) {
            findUnitOrThrow(unitId);
            accounts = repository.findByUnitId(unitId);
        } else if (supplierId != null) {
            findSupplierOrThrow(supplierId);
            accounts = repository.findBySupplierId(supplierId);
        } else {
            accounts = repository.findAll();
        }

        if (type != null) {
            accounts = accounts.stream().filter(a -> a.getType() == type).toList();
        }
        if (paid != null) {
            accounts = accounts.stream().filter(a -> a.isPaid() == paid).toList();
        }
        if (Boolean.TRUE.equals(overdue)) {
            LocalDate today = LocalDate.now();
            accounts = accounts.stream()
                    .filter(a -> !a.isPaid() && a.getDueDate().isBefore(today))
                    .toList();
        }
        if (dueYearMonth != null) {
            YearMonth month = parseYearMonth(dueYearMonth, "vencimento");
            accounts = accounts.stream()
                    .filter(a -> YearMonth.from(a.getDueDate()).equals(month))
                    .toList();
        }
        if (paymentYearMonth != null) {
            YearMonth month = parseYearMonth(paymentYearMonth, "pagamento");
            accounts = accounts.stream()
                    .filter(a -> a.isPaid() && YearMonth.from(a.getPaymentDate()).equals(month))
                    .toList();
        }
        return accounts;
    }

    public Account findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Conta não encontrada."));
    }

    public Account update(
            Long id,
            AccountType type,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Fund fund,
            boolean recurring,
            Long unitId,
            Long supplierId,
            LocalDate paymentDate,
            String observations) {
        Account account = findById(id);
        if (account.getType() != type) {
            throw new AccountTypeChangeNotAllowedException();
        }
        validateNonNegativeAmount(amount);
        Unit unit = resolveUnit(type, unitId, supplierId);
        Supplier supplier = resolveSupplier(type, unitId, supplierId);
        account.setAmount(amount);
        account.setDueDate(dueDate);
        account.setDescription(description);
        account.setFund(fund);
        account.setRecurring(recurring);
        account.setUnit(unit);
        account.setSupplier(supplier);
        account.setPaymentDate(paymentDate);
        account.setObservations(observations);
        return repository.save(account);
    }

    public Account registerPayment(Long id, LocalDate paymentDate) {
        Account account = findById(id);
        account.setPaymentDate(paymentDate);
        return repository.save(account);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Conta não encontrada.");
        }
        repository.deleteById(id);
    }

    private void validateNonNegativeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountAmountException();
        }
    }

    private Unit resolveUnit(AccountType type, Long unitId, Long supplierId) {
        if (type == AccountType.RECEIVABLE) {
            if (unitId == null) {
                throw new BadRequestException("A unidade é obrigatória para uma conta a receber.");
            }
            if (supplierId != null) {
                throw new BadRequestException(
                        "Uma conta a receber não pode informar um fornecedor.");
            }
            return findUnitOrThrow(unitId);
        }
        if (unitId != null) {
            throw new BadRequestException("Uma conta a pagar não pode informar uma unidade.");
        }
        return null;
    }

    private Supplier resolveSupplier(AccountType type, Long unitId, Long supplierId) {
        if (type == AccountType.PAYABLE) {
            if (supplierId == null) {
                throw new BadRequestException("O fornecedor é obrigatório para uma conta a pagar.");
            }
            return findSupplierOrThrow(supplierId);
        }
        return null;
    }

    private Unit findUnitOrThrow(Long unitId) {
        return unitRepository
                .findById(unitId)
                .orElseThrow(() -> new NotFoundException(
                        "Unidade não encontrada. Cadastre uma unidade antes de lançar uma conta a receber."));
    }

    private Supplier findSupplierOrThrow(Long supplierId) {
        return supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new NotFoundException(
                        "Fornecedor não encontrado. Cadastre um fornecedor antes de lançar uma conta a pagar."));
    }

    private YearMonth parseYearMonth(String value, String fieldLabel) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Mês/ano de " + fieldLabel + " inválido. Use o formato AAAA-MM.");
        }
    }
}
