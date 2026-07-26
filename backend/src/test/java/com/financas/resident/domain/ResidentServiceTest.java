package com.financas.resident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class ResidentServiceTest {

    @Mock
    private ResidentRepository repository;

    @Mock
    private UnitRepository unitRepository;

    private ResidentService service;

    @BeforeEach
    void setUp() {
        service = new ResidentService(repository, unitRepository);
    }

    @Test
    void createsResidentWhenUnitExists() {
        Unit unit = withId(new Unit("Bloco A - 101"), 1L);
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.save(any(Resident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Resident created = service.create("Maria Silva", 1L, null, null);

        assertThat(created.getName()).isEqualTo("Maria Silva");
        assertThat(created.getUnit()).isEqualTo(unit);
    }

    @Test
    void rejectsCreateWhenUnitDoesNotExist() {
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Maria Silva", 999L, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdThrowsWhenResidentDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsUpdateWhenNewUnitDoesNotExist() {
        Unit currentUnit = withId(new Unit("Bloco A - 101"), 1L);
        Resident existing = withId(new Resident("Maria Silva", currentUnit, null, null), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(unitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, "Maria Silva", 999L, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesExistingResident() {
        when(repository.existsById(10L)).thenReturn(true);

        service.delete(10L);

        verify(repository).deleteById(10L);
    }

    @Test
    void deleteThrowsWhenResidentDoesNotExist() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    private Unit withId(Unit unit, Long id) {
        ReflectionTestUtils.setField(unit, "id", id);
        return unit;
    }

    private Resident withId(Resident resident, Long id) {
        ReflectionTestUtils.setField(resident, "id", id);
        return resident;
    }
}
