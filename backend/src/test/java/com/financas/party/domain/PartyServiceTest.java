package com.financas.party.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.account.domain.AccountRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @Mock
    private PartyRepository repository;

    @Mock
    private AccountRepository accountRepository;

    private PartyService service;

    @BeforeEach
    void setUp() {
        service = new PartyService(repository, accountRepository);
    }

    @Test
    void createsPartyWhenNameIsNotDuplicated() {
        when(repository.findByNormalizedName("Bloco A - 101")).thenReturn(Optional.empty());
        when(repository.save(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Party created = service.create("Bloco A - 101", "12.345.678/0001-90");

        assertThat(created.getName()).isEqualTo("Bloco A - 101");
        assertThat(created.getPixKey()).isEqualTo("12.345.678/0001-90");
    }

    @Test
    void rejectsCreateWhenNameIsDuplicatedIgnoringCaseAndWhitespace() {
        Party existing = withId(new Party("Bloco A - 101", null), 1L);
        when(repository.findByNormalizedName(" bloco a - 101 ")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(" bloco a - 101 ", null))
                .isInstanceOf(DuplicatePartyException.class);
    }

    @Test
    void allowsUpdateKeepingTheSameNameOnTheSameParty() {
        Party existing = withId(new Party("Bloco A - 101", null), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByNormalizedName("Bloco A - 101")).thenReturn(Optional.of(existing));
        when(repository.save(any(Party.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Party updated = service.update(1L, "Bloco A - 101", "chave-pix");

        assertThat(updated.getPixKey()).isEqualTo("chave-pix");
    }

    @Test
    void rejectsUpdateWhenNameBelongsToAnotherParty() {
        Party current = withId(new Party("Bloco A - 101", null), 1L);
        Party other = withId(new Party("Bloco A - 102", null), 2L);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.findByNormalizedName("Bloco A - 102")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L, "Bloco A - 102", null))
                .isInstanceOf(DuplicatePartyException.class);
    }

    @Test
    void findByIdThrowsWhenPartyDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesPartyWithoutAccounts() {
        Party existing = withId(new Party("Bloco A - 101", null), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByPartyId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteRejectsPartyWithAccountsLinked() {
        Party existing = withId(new Party("Bloco A - 101", null), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByPartyId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(PartyHasAccountsException.class);

        verify(repository, never()).deleteById(any());
    }

    private Party withId(Party party, Long id) {
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }
}
