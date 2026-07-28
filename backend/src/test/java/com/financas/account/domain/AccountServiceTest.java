package com.financas.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.shared.exceptions.BadRequestException;
import com.financas.shared.exceptions.NotFoundException;
import com.financas.supplier.domain.Supplier;
import com.financas.supplier.domain.SupplierRepository;
import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private SupplierRepository supplierRepository;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(repository, unitRepository, supplierRepository);
    }

    @Test
    void createsReceivableWithAllFieldsWhenUnitExists() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(
                AccountType.RECEIVABLE,
                new BigDecimal("350.00"),
                LocalDate.of(2026, 8, 10),
                "Taxa condominial - Agosto/2026",
                Fund.POOL,
                true,
                1L,
                null,
                null,
                null);

        assertThat(created.getType()).isEqualTo(AccountType.RECEIVABLE);
        assertThat(created.getAmount()).isEqualByComparingTo("350.00");
        assertThat(created.getUnit()).isEqualTo(unit);
        assertThat(created.getSupplier()).isNull();
        assertThat(created.isPaid()).isFalse();
    }

    @Test
    void createsPayableWithAllFieldsWhenSupplierExists() {
        Supplier supplier = withId(new Supplier("Empresa de Limpeza XYZ", null, null), 1L);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(
                AccountType.PAYABLE,
                new BigDecimal("400.00"),
                LocalDate.of(2026, 8, 5),
                "Limpeza - Agosto/2026",
                Fund.SIDE_GARDEN,
                false,
                null,
                1L,
                null,
                "Disse que vai pagar mês que vem");

        assertThat(created.getType()).isEqualTo(AccountType.PAYABLE);
        assertThat(created.getSupplier()).isEqualTo(supplier);
        assertThat(created.getUnit()).isNull();
        assertThat(created.getObservations()).isEqualTo("Disse que vai pagar mês que vem");
    }

    @Test
    void createAcceptsZeroAmount() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(
                AccountType.RECEIVABLE,
                BigDecimal.ZERO,
                LocalDate.now(),
                "Taxa",
                Fund.POOL,
                true,
                1L,
                null,
                null,
                null);

        assertThat(created.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsCreateWhenAmountIsNegative() {
        assertThatThrownBy(() -> service.create(
                        AccountType.RECEIVABLE,
                        new BigDecimal("-10.00"),
                        LocalDate.now(),
                        "Taxa",
                        Fund.POOL,
                        true,
                        1L,
                        null,
                        null,
                        null))
                .isInstanceOf(InvalidAccountAmountException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsReceivableWithoutUnit() {
        assertThatThrownBy(() -> service.create(
                        AccountType.RECEIVABLE,
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        Fund.POOL,
                        true,
                        null,
                        null,
                        null,
                        null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsReceivableWithSupplier() {
        assertThatThrownBy(() -> service.create(
                        AccountType.RECEIVABLE,
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        Fund.POOL,
                        true,
                        1L,
                        1L,
                        null,
                        null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPayableWithoutSupplier() {
        assertThatThrownBy(() -> service.create(
                        AccountType.PAYABLE,
                        new BigDecimal("400.00"),
                        LocalDate.now(),
                        "Limpeza",
                        Fund.SIDE_GARDEN,
                        false,
                        null,
                        null,
                        null,
                        null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPayableWithUnit() {
        assertThatThrownBy(() -> service.create(
                        AccountType.PAYABLE,
                        new BigDecimal("400.00"),
                        LocalDate.now(),
                        "Limpeza",
                        Fund.SIDE_GARDEN,
                        false,
                        1L,
                        1L,
                        null,
                        null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateRejectsTypeChange() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Account existing = withId(receivable(unit, new BigDecimal("350.00"), LocalDate.now()), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(
                        10L,
                        AccountType.PAYABLE,
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        Fund.POOL,
                        true,
                        null,
                        1L,
                        null,
                        null))
                .isInstanceOf(AccountTypeChangeNotAllowedException.class);
    }

    @Test
    void createForAllUnitsAlwaysCreatesReceivableType() {
        Unit unitA = withId(new Unit("Bloco A - 101"), 1L);
        Unit unitB = withId(new Unit("Bloco A - 102"), 2L);
        when(unitRepository.findAll()).thenReturn(List.of(unitA, unitB));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Account> created = service.createForAllUnits(
                new BigDecimal("350.00"), LocalDate.of(2026, 8, 10), "Taxa", Fund.POOL, true, null, null);

        assertThat(created).hasSize(2);
        assertThat(created).allSatisfy(account -> assertThat(account.getType()).isEqualTo(AccountType.RECEIVABLE));
        assertThat(created).extracting(Account::getUnit).containsExactly(unitA, unitB);
    }

    @Test
    void rejectsCreateForAllUnitsWhenNoUnitsAreRegistered() {
        when(unitRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.createForAllUnits(
                        new BigDecimal("350.00"), LocalDate.now(), "Taxa", Fund.POOL, true, null, null))
                .isInstanceOf(NoUnitsRegisteredException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void findAllFiltersByTypeAlone() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Supplier supplier = withId(new Supplier("Fornecedor", null, null), 1L);
        Account receivableAccount = withId(receivable(unit, new BigDecimal("350.00"), LocalDate.now()), 1L);
        Account payableAccount = withId(payable(supplier, new BigDecimal("400.00"), LocalDate.now()), 2L);
        when(repository.findAll()).thenReturn(List.of(receivableAccount, payableAccount));

        assertThat(service.findAll(null, null, AccountType.PAYABLE, null, null, null, null))
                .containsExactly(payableAccount);
        assertThat(service.findAll(null, null, AccountType.RECEIVABLE, null, null, null, null))
                .containsExactly(receivableAccount);
    }

    @Test
    void findAllCombinesTypeWithSupplierId() {
        Supplier supplier = withId(new Supplier("Fornecedor", null, null), 1L);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        Account payableAccount = withId(payable(supplier, new BigDecimal("400.00"), LocalDate.now()), 2L);
        when(repository.findBySupplierId(1L)).thenReturn(List.of(payableAccount));

        assertThat(service.findAll(null, 1L, AccountType.PAYABLE, null, null, null, null))
                .containsExactly(payableAccount);
    }

    @Test
    void findAllCombinesTypeWithUnitIdPaidAndOverdue() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        Account overdueReceivable =
                withId(receivable(unit, new BigDecimal("350.00"), LocalDate.now().minusDays(1)), 1L);
        Account paidReceivable = withId(
                receivableWithPayment(unit, new BigDecimal("350.00"), LocalDate.now().minusDays(1), LocalDate.now()),
                2L);
        when(repository.findByUnitId(1L)).thenReturn(List.of(overdueReceivable, paidReceivable));

        assertThat(service.findAll(1L, null, AccountType.RECEIVABLE, false, true, null, null))
                .containsExactly(overdueReceivable);
    }

    @Test
    void findAllThrowsWhenUnitIdFilterDoesNotExist() {
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAll(999L, null, null, null, null, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllThrowsWhenSupplierIdFilterDoesNotExist() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAll(null, 999L, null, null, null, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdUpdateAndDeleteThrowWhenAccountDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(
                        999L,
                        AccountType.RECEIVABLE,
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        Fund.POOL,
                        true,
                        1L,
                        null,
                        null,
                        null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void registerPaymentMarksPayableAccountAsPaid() {
        Supplier supplier = withId(new Supplier("Fornecedor", null, null), 1L);
        Account existing = withId(payable(supplier, new BigDecimal("400.00"), LocalDate.now()), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account paid = service.registerPayment(10L, LocalDate.of(2026, 8, 15));

        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void overdueFilterIncludesPendingPayableAndExcludesPaidPayable() {
        Supplier supplier = withId(new Supplier("Fornecedor", null, null), 1L);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Account overduePending = withId(payable(supplier, new BigDecimal("400.00"), yesterday), 1L);
        Account overduePaid =
                withId(payableWithPayment(supplier, new BigDecimal("400.00"), yesterday, LocalDate.now()), 2L);
        when(repository.findAll()).thenReturn(List.of(overduePending, overduePaid));

        assertThat(service.findAll(null, null, null, null, true, null, null)).containsExactly(overduePending);
    }

    private Account receivable(Unit unit, BigDecimal amount, LocalDate dueDate) {
        return new Account(AccountType.RECEIVABLE, amount, dueDate, "Taxa", Fund.POOL, true, unit, null, null, null);
    }

    private Account receivableWithPayment(Unit unit, BigDecimal amount, LocalDate dueDate, LocalDate paymentDate) {
        return new Account(
                AccountType.RECEIVABLE, amount, dueDate, "Taxa", Fund.POOL, true, unit, null, paymentDate, null);
    }

    private Account payable(Supplier supplier, BigDecimal amount, LocalDate dueDate) {
        return new Account(
                AccountType.PAYABLE, amount, dueDate, "Limpeza", Fund.SIDE_GARDEN, false, null, supplier, null, null);
    }

    private Account payableWithPayment(
            Supplier supplier, BigDecimal amount, LocalDate dueDate, LocalDate paymentDate) {
        return new Account(
                AccountType.PAYABLE,
                amount,
                dueDate,
                "Limpeza",
                Fund.SIDE_GARDEN,
                false,
                null,
                supplier,
                paymentDate,
                null);
    }

    private Unit withId(Unit unit, Long id) {
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private Supplier withId(Supplier supplier, Long id) {
        ReflectionTestUtils.setField(supplier, "id", id);
        return supplier;
    }

    private Account withId(Account account, Long id) {
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
