package com.financas.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.account.domain.AccountRepository;
import com.financas.shared.exceptions.NotFoundException;
import com.financas.supplier.domain.SupplierRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock
    private UnitRepository repository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SupplierRepository supplierRepository;

    private UnitService service;

    @BeforeEach
    void setUp() {
        service = new UnitService(repository, accountRepository, supplierRepository);
    }

    @Test
    void createsUnitWhenIdentifierIsNotDuplicated() {
        when(repository.findByNormalizedIdentifier("Bloco A - 101")).thenReturn(Optional.empty());
        when(repository.save(any(Unit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Unit created = service.create("Bloco A - 101");

        assertThat(created.getIdentifier()).isEqualTo("Bloco A - 101");
    }

    @Test
    void rejectsCreateWhenIdentifierIsDuplicatedIgnoringCaseAndWhitespace() {
        Unit existing = withId(new Unit("Bloco A - 101"), 1L);
        when(repository.findByNormalizedIdentifier("bloco a - 101 ")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("bloco a - 101 "))
                .isInstanceOf(DuplicateUnitException.class);
    }

    @Test
    void allowsUpdateKeepingTheSameIdentifierOnTheSameUnit() {
        Unit existing = withId(new Unit("Bloco A - 101"), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByNormalizedIdentifier("Bloco A - 101")).thenReturn(Optional.of(existing));
        when(repository.save(any(Unit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Unit updated = service.update(1L, "Bloco A - 101");

        assertThat(updated.getIdentifier()).isEqualTo("Bloco A - 101");
    }

    @Test
    void rejectsUpdateWhenIdentifierBelongsToAnotherUnit() {
        Unit current = withId(new Unit("Bloco A - 101"), 1L);
        Unit other = withId(new Unit("Bloco A - 102"), 2L);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.findByNormalizedIdentifier("Bloco A - 102")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L, "Bloco A - 102"))
                .isInstanceOf(DuplicateUnitException.class);
    }

    @Test
    void findByIdThrowsWhenUnitDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesUnitWithoutAccountsOrSuppliers() {
        Unit existing = withId(new Unit("Bloco A - 101"), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByUnitId(1L)).thenReturn(false);
        when(supplierRepository.existsByUnitId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteRejectsUnitWithAccountsLinked() {
        Unit existing = withId(new Unit("Bloco A - 101"), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByUnitId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(UnitHasAccountsException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsUnitWithSuppliersLinked() {
        Unit existing = withId(new Unit("Bloco A - 101"), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByUnitId(1L)).thenReturn(false);
        when(supplierRepository.existsByUnitId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(UnitHasSuppliersException.class);

        verify(repository, never()).deleteById(any());
    }

    private Unit withId(Unit unit, Long id) {
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }
}
