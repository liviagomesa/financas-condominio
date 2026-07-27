package com.financas.receivable.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.shared.exceptions.BadRequestException;
import com.financas.shared.exceptions.NotFoundException;
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
class ReceivableServiceTest {

    @Mock
    private ReceivableRepository repository;

    @Mock
    private UnitRepository unitRepository;

    private ReceivableService service;

    @BeforeEach
    void setUp() {
        service = new ReceivableService(repository, unitRepository);
    }

    @Test
    void createsReceivableWithAllFieldsWhenUnitExists() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Receivable created = service.create(
                new BigDecimal("350.00"),
                LocalDate.of(2026, 8, 10),
                "Taxa condominial - Agosto/2026",
                TargetAccount.POOL,
                true,
                1L,
                null);

        assertThat(created.getAmount()).isEqualByComparingTo("350.00");
        assertThat(created.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(created.getDescription()).isEqualTo("Taxa condominial - Agosto/2026");
        assertThat(created.getTargetAccount()).isEqualTo(TargetAccount.POOL);
        assertThat(created.isRecurring()).isTrue();
        assertThat(created.getUnit()).isEqualTo(unit);
        assertThat(created.isPaid()).isFalse();
    }

    @Test
    void createsReceivableAlreadyPaidWhenPaymentDateIsInformed() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Receivable created = service.create(
                new BigDecimal("350.00"),
                LocalDate.of(2026, 8, 10),
                "Taxa condominial - Agosto/2026",
                TargetAccount.POOL,
                true,
                1L,
                LocalDate.of(2026, 8, 5));

        assertThat(created.isPaid()).isTrue();
        assertThat(created.getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    void rejectsCreateWhenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> service.create(
                        BigDecimal.ZERO, LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L, null))
                .isInstanceOf(InvalidReceivableAmountException.class);

        assertThatThrownBy(() -> service.create(
                        new BigDecimal("-10.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L, null))
                .isInstanceOf(InvalidReceivableAmountException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsCreateWhenUnitDoesNotExist() {
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                        new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 999L, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createForAllUnitsCreatesOneReceivablePerRegisteredUnit() {
        Unit unitA = withId(new Unit("Bloco A - 101"), 1L);
        Unit unitB = withId(new Unit("Bloco A - 102"), 2L);
        when(unitRepository.findAll()).thenReturn(List.of(unitA, unitB));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Receivable> created = service.createForAllUnits(
                new BigDecimal("350.00"),
                LocalDate.of(2026, 8, 10),
                "Taxa condominial - Agosto/2026",
                TargetAccount.POOL,
                true,
                null);

        assertThat(created).hasSize(2);
        assertThat(created).extracting(Receivable::getUnit).containsExactly(unitA, unitB);
        assertThat(created).allSatisfy(receivable -> {
            assertThat(receivable.getAmount()).isEqualByComparingTo("350.00");
            assertThat(receivable.getTargetAccount()).isEqualTo(TargetAccount.POOL);
            assertThat(receivable.isPaid()).isFalse();
        });
    }

    @Test
    void createForAllUnitsCreatesAlreadyPaidWhenPaymentDateIsInformed() {
        Unit unitA = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findAll()).thenReturn(List.of(unitA));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Receivable> created = service.createForAllUnits(
                new BigDecimal("350.00"),
                LocalDate.of(2026, 8, 10),
                "Taxa condominial - Agosto/2026",
                TargetAccount.POOL,
                true,
                LocalDate.of(2026, 8, 5));

        assertThat(created).allSatisfy(receivable -> assertThat(receivable.isPaid()).isTrue());
    }

    @Test
    void rejectsCreateForAllUnitsWhenNoUnitsAreRegistered() {
        when(unitRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.createForAllUnits(
                        new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, null))
                .isInstanceOf(NoUnitsRegisteredException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesAmountWhenNewValueIsPositive() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable existing = withId(
                new Receivable(new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, unit),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Receivable updated = service.update(
                10L, new BigDecimal("370.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L, null);

        assertThat(updated.getAmount()).isEqualByComparingTo("370.00");
    }

    @Test
    void rejectsUpdateWhenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> service.update(
                        10L, BigDecimal.ZERO, LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L, null))
                .isInstanceOf(InvalidReceivableAmountException.class);

        verify(repository, never()).findById(any());
    }

    @Test
    void updateChangesAssociatedUnitWhenNewUnitExists() {
        Unit originalUnit = withId(new Unit("Bloco A - 101"), 1L);
        Unit newUnit = withId(new Unit("Bloco A - 102"), 2L);
        Receivable existing = withId(
                new Receivable(
                        new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, originalUnit),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(unitRepository.findById(2L)).thenReturn(Optional.of(newUnit));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Receivable updated = service.update(
                10L, new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 2L, null);

        assertThat(updated.getUnit()).isEqualTo(newUnit);
    }

    @Test
    void updateThrowsWhenNewUnitDoesNotExist() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable existing = withId(
                new Receivable(new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, unit),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        10L, new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 999L, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrowsWhenReceivableDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        999L, new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void registerPaymentMarksPendingReceivableAsPaid() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable existing = withId(
                new Receivable(new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, unit),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Receivable paid = service.registerPayment(10L, LocalDate.of(2026, 8, 15));

        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void registerPaymentAgainUpdatesPaymentDateWithoutReverting() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable existing = withId(
                new Receivable(
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        TargetAccount.POOL,
                        true,
                        unit,
                        LocalDate.of(2026, 8, 15)),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Receivable updated = service.registerPayment(10L, LocalDate.of(2026, 8, 20));

        assertThat(updated.getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 20));
    }

    @Test
    void registerPaymentThrowsWhenReceivableDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerPayment(999L, LocalDate.now()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void editingOrRemovingPaidReceivableIsAllowedWithoutRestriction() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable paidReceivable = withId(
                new Receivable(
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        TargetAccount.POOL,
                        true,
                        unit,
                        LocalDate.now()),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(paidReceivable));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Receivable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.existsById(10L)).thenReturn(true);

        Receivable updated = service.update(
                10L, new BigDecimal("400.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L, LocalDate.now());
        assertThat(updated.getAmount()).isEqualByComparingTo("400.00");

        service.delete(10L);
        verify(repository).deleteById(10L);
    }

    @Test
    void deleteRemovesExistingReceivable() {
        when(repository.existsById(10L)).thenReturn(true);

        service.delete(10L);

        verify(repository).deleteById(10L);
    }

    @Test
    void deleteThrowsWhenReceivableDoesNotExist() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void findAllFiltersByPaidStatus() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable pending = withId(
                new Receivable(new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, unit),
                1L);
        Receivable paid = withId(
                new Receivable(
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        TargetAccount.POOL,
                        true,
                        unit,
                        LocalDate.now()),
                2L);
        when(repository.findAll()).thenReturn(List.of(pending, paid));

        assertThat(service.findAll(null, true, null, null, null)).containsExactly(paid);
        assertThat(service.findAll(null, false, null, null, null)).containsExactly(pending);
    }

    @Test
    void findAllFiltersOverdueExcludingPaidReceivables() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Receivable overduePending = withId(
                new Receivable(new BigDecimal("350.00"), yesterday, "Taxa", TargetAccount.POOL, true, unit), 1L);
        Receivable overduePaid = withId(
                new Receivable(
                        new BigDecimal("350.00"), yesterday, "Taxa", TargetAccount.POOL, true, unit, LocalDate.now()),
                2L);
        Receivable notYetDue = withId(
                new Receivable(new BigDecimal("350.00"), tomorrow, "Taxa", TargetAccount.POOL, true, unit), 3L);
        when(repository.findAll()).thenReturn(List.of(overduePending, overduePaid, notYetDue));

        assertThat(service.findAll(null, null, true, null, null)).containsExactly(overduePending);
    }

    @Test
    void findAllFiltersByDueYearMonth() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable august = withId(
                new Receivable(
                        new BigDecimal("350.00"),
                        LocalDate.of(2026, 8, 10),
                        "Taxa",
                        TargetAccount.POOL,
                        true,
                        unit),
                1L);
        Receivable september = withId(
                new Receivable(
                        new BigDecimal("350.00"),
                        LocalDate.of(2026, 9, 10),
                        "Taxa",
                        TargetAccount.POOL,
                        true,
                        unit),
                2L);
        when(repository.findAll()).thenReturn(List.of(august, september));

        assertThat(service.findAll(null, null, null, "2026-08", null)).containsExactly(august);
    }

    @Test
    void findAllFiltersByPaymentYearMonth() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        Receivable paidInAugust = withId(
                new Receivable(
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        TargetAccount.POOL,
                        true,
                        unit,
                        LocalDate.of(2026, 8, 15)),
                1L);
        Receivable pending = withId(
                new Receivable(new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, unit),
                2L);
        when(repository.findAll()).thenReturn(List.of(paidInAugust, pending));

        assertThat(service.findAll(null, null, null, null, "2026-08")).containsExactly(paidInAugust);
    }

    @Test
    void findAllRejectsMalformedYearMonth() {
        when(repository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.findAll(null, null, null, "not-a-month", null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.findAll(null, null, null, null, "not-a-month"))
                .isInstanceOf(BadRequestException.class);
    }

    private Unit withId(Unit unit, Long id) {
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private Receivable withId(Receivable receivable, Long id) {
        ReflectionTestUtils.setField(receivable, "id", id);
        return receivable;
    }
}
