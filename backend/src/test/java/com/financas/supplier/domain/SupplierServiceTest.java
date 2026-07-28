package com.financas.supplier.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.account.domain.AccountRepository;
import com.financas.shared.exceptions.ConflictException;
import com.financas.shared.exceptions.NotFoundException;
import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository repository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private AccountRepository accountRepository;

    private SupplierService service;

    @BeforeEach
    void setUp() {
        service = new SupplierService(repository, unitRepository, accountRepository);
    }

    @Test
    void createsSupplierWithoutUnitOrPixKey() {
        when(repository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier created = service.create("Empresa de Limpeza XYZ", null, null);

        assertThat(created.getName()).isEqualTo("Empresa de Limpeza XYZ");
        assertThat(created.getUnit()).isNull();
        assertThat(created.getPixKey()).isNull();
    }

    @Test
    void createsSupplierWithUnitAndPixKey() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier created = service.create("Síndico Bloco A - 101", 1L, "12.345.678/0001-90");

        assertThat(created.getUnit()).isEqualTo(unit);
        assertThat(created.getPixKey()).isEqualTo("12.345.678/0001-90");
    }

    @Test
    void createThrowsWhenUnitDoesNotExist() {
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Fornecedor", 999L, null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdUpdateAndDeleteThrowWhenSupplierDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.update(999L, "Fornecedor", null, null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteIsBlockedWhenSupplierHasLinkedAccount() {
        Supplier supplier = withId(new Supplier("Fornecedor", null, null), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        when(accountRepository.existsBySupplierId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteIsAllowedWhenSupplierHasNoLinkedAccount() {
        Supplier supplier = withId(new Supplier("Fornecedor", null, null), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(supplier));
        when(accountRepository.existsBySupplierId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private Unit withId(Unit unit, Long id) {
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private Supplier withId(Supplier supplier, Long id) {
        ReflectionTestUtils.setField(supplier, "id", id);
        return supplier;
    }
}
