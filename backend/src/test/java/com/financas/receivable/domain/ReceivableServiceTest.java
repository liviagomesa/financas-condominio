package com.financas.receivable.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                1L);

        assertThat(created.getAmount()).isEqualByComparingTo("350.00");
        assertThat(created.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(created.getDescription()).isEqualTo("Taxa condominial - Agosto/2026");
        assertThat(created.getTargetAccount()).isEqualTo(TargetAccount.POOL);
        assertThat(created.isRecurring()).isTrue();
        assertThat(created.getUnit()).isEqualTo(unit);
    }

    @Test
    void rejectsCreateWhenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> service.create(
                        BigDecimal.ZERO, LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L))
                .isInstanceOf(InvalidReceivableAmountException.class);

        assertThatThrownBy(() -> service.create(
                        new BigDecimal("-10.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L))
                .isInstanceOf(InvalidReceivableAmountException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsCreateWhenUnitDoesNotExist() {
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                        new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 999L))
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
                true);

        assertThat(created).hasSize(2);
        assertThat(created).extracting(Receivable::getUnit).containsExactly(unitA, unitB);
        assertThat(created).allSatisfy(receivable -> {
            assertThat(receivable.getAmount()).isEqualByComparingTo("350.00");
            assertThat(receivable.getTargetAccount()).isEqualTo(TargetAccount.POOL);
        });
    }

    @Test
    void rejectsCreateForAllUnitsWhenNoUnitsAreRegistered() {
        when(unitRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.createForAllUnits(
                        new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true))
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
                10L, new BigDecimal("370.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L);

        assertThat(updated.getAmount()).isEqualByComparingTo("370.00");
    }

    @Test
    void rejectsUpdateWhenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> service.update(
                        10L, BigDecimal.ZERO, LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L))
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
                10L, new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 2L);

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
                        10L, new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateThrowsWhenReceivableDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                        999L, new BigDecimal("350.00"), LocalDate.now(), "Taxa", TargetAccount.POOL, true, 1L))
                .isInstanceOf(NotFoundException.class);
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

    private Unit withId(Unit unit, Long id) {
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private Receivable withId(Receivable receivable, Long id) {
        ReflectionTestUtils.setField(receivable, "id", id);
        return receivable;
    }
}
