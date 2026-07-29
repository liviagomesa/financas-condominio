package com.financas.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository repository;

    @Mock
    private PartyRepository partyRepository;

    private GroupService service;

    @BeforeEach
    void setUp() {
        service = new GroupService(repository, partyRepository);
    }

    @Test
    void createsGroupResolvingPartyIds() {
        Party partyA = withId(new Party("Bloco A - 101", null), 1L);
        Party partyB = withId(new Party("Bloco A - 102", null), 2L);
        when(repository.findByNormalizedName("Bloco A")).thenReturn(Optional.empty());
        when(partyRepository.findById(1L)).thenReturn(Optional.of(partyA));
        when(partyRepository.findById(2L)).thenReturn(Optional.of(partyB));
        when(repository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Group created = service.create("Bloco A", List.of(1L, 2L));

        assertThat(created.getName()).isEqualTo("Bloco A");
        assertThat(created.getMembers()).containsExactlyInAnyOrder(partyA, partyB);
    }

    @Test
    void rejectsCreateWhenNameIsDuplicated() {
        Group existing = withId(new Group("Bloco A", Set.of()), 1L);
        when(repository.findByNormalizedName("Bloco A")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("Bloco A", List.of()))
                .isInstanceOf(DuplicateGroupException.class);
    }

    @Test
    void rejectsCreateWhenPartyIdDoesNotExist() {
        when(repository.findByNormalizedName("Bloco A")).thenReturn(Optional.empty());
        when(partyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Bloco A", List.of(999L)))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updateReplacesMembersEntirely() {
        Party partyA = withId(new Party("Bloco A - 101", null), 1L);
        Party partyC = withId(new Party("Bloco A - 103", null), 3L);
        Group existing = withId(new Group("Bloco A", new java.util.HashSet<>(Set.of(partyA))), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByNormalizedName("Bloco A")).thenReturn(Optional.of(existing));
        when(partyRepository.findById(3L)).thenReturn(Optional.of(partyC));
        when(repository.save(any(Group.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Group updated = service.update(1L, "Bloco A", List.of(3L));

        assertThat(updated.getMembers()).containsExactly(partyC);
    }

    @Test
    void deleteAlwaysAllowedEvenWithMembers() {
        Party partyA = withId(new Party("Bloco A - 101", null), 1L);
        Group existing = withId(new Group("Bloco A", Set.of(partyA)), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteAllowedWhenGroupHasNoMembers() {
        Group existing = withId(new Group("Bloco A", Set.of()), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private Group withId(Group group, Long id) {
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private Party withId(Party party, Long id) {
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }
}
