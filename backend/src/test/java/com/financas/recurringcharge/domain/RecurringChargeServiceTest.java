package com.financas.recurringcharge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountRepository;
import com.financas.account.domain.AccountType;
import com.financas.fund.domain.Fund;
import com.financas.fund.domain.FundRepository;
import com.financas.group.domain.Group;
import com.financas.group.domain.GroupRepository;
import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.math.BigDecimal;
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
class RecurringChargeServiceTest {

    @Mock
    private RecurringChargeRepository repository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private FundRepository fundRepository;

    private RecurringChargeService service;

    @BeforeEach
    void setUp() {
        service = new RecurringChargeService(repository, partyRepository, groupRepository, fundRepository);
    }

    @Test
    void createsRecurringChargeForSpecificParty() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        stubFund(1L);
        when(repository.save(any(RecurringCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringCharge created =
                service.create(AccountType.RECEIVABLE, new BigDecimal("350.00"), 10, "Taxa condominial", 1L, 1L, null);

        assertThat(created.getType()).isEqualTo(AccountType.RECEIVABLE);
        assertThat(created.getAmount()).isEqualByComparingTo("350.00");
        assertThat(created.getDueDay()).isEqualTo(10);
        assertThat(created.getParty()).isEqualTo(party);
        assertThat(created.isActive()).isTrue();
        assertThat(created.isLastGenerationFailed()).isFalse();
    }

    @Test
    void createForGroupCreatesOneRecurringChargePerMember() {
        Party partyA = withId(new Party("Bloco A - 101", null), 1L);
        Party partyB = withId(new Party("Bloco A - 102", null), 2L);
        Group group = withId(new Group("Bloco A", Set.of(partyA, partyB)), 1L);
        stubFund(1L);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(repository.save(any(RecurringCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecurringCharge> created =
                service.createForGroup(AccountType.RECEIVABLE, new BigDecimal("350.00"), 10, "Taxa", 1L, 1L, null);

        assertThat(created).hasSize(2);
        assertThat(created).extracting(RecurringCharge::getParty).containsExactlyInAnyOrder(partyA, partyB);
    }

    @Test
    void rejectsCreateForGroupWhenGroupIsEmpty() {
        Group group = withId(new Group("Bloco A", Set.of()), 1L);
        stubFund(1L);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() ->
                        service.createForGroup(AccountType.RECEIVABLE, new BigDecimal("350.00"), 10, "Taxa", 1L, 1L, null))
                .isInstanceOf(EmptyGroupException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void createAndCreateForGroupAcceptZeroAmount() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        Group group = withId(new Group("Bloco A", Set.of(party)), 1L);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        stubFund(1L);
        when(repository.save(any(RecurringCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringCharge created = service.create(AccountType.RECEIVABLE, BigDecimal.ZERO, 10, "Taxa", 1L, 1L, null);
        List<RecurringCharge> createdForGroup =
                service.createForGroup(AccountType.RECEIVABLE, BigDecimal.ZERO, 10, "Taxa", 1L, 1L, null);

        assertThat(created.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(createdForGroup).allSatisfy(rc -> assertThat(rc.getAmount()).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void createAndCreateForGroupRejectNegativeAmount() {
        assertThatThrownBy(() ->
                        service.create(AccountType.RECEIVABLE, new BigDecimal("-10.00"), 10, "Taxa", 1L, 1L, null))
                .isInstanceOf(InvalidRecurringChargeAmountException.class);
        assertThatThrownBy(() -> service.createForGroup(
                        AccountType.RECEIVABLE, new BigDecimal("-10.00"), 10, "Taxa", 1L, 1L, null))
                .isInstanceOf(InvalidRecurringChargeAmountException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updateCreatesNewActiveLineAndDeactivatesThePrevious() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = stubFund(1L);
        RecurringCharge existing = withId(
                new RecurringCharge(AccountType.RECEIVABLE, new BigDecimal("300.00"), 10, "Taxa", fund, party, null),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        when(repository.save(any(RecurringCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecurringCharge updated = service.update(
                10L, AccountType.RECEIVABLE, new BigDecimal("330.00"), 10, "Taxa", 1L, 1L, null);

        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDeactivatedAt()).isNotNull();
        assertThat(updated).isNotSameAs(existing);
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getAmount()).isEqualByComparingTo("330.00");
    }

    @Test
    void updateRejectsTypeChange() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        RecurringCharge existing = withId(
                new RecurringCharge(AccountType.RECEIVABLE, new BigDecimal("300.00"), 10, "Taxa", fund, party, null),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(
                        10L, AccountType.PAYABLE, new BigDecimal("300.00"), 10, "Taxa", 1L, 1L, null))
                .isInstanceOf(RecurringChargeTypeChangeNotAllowedException.class);
    }

    @Test
    void updateRejectsNegativeAmountWithoutAlteringExistingLine() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        RecurringCharge existing = withId(
                new RecurringCharge(AccountType.RECEIVABLE, new BigDecimal("300.00"), 10, "Taxa", fund, party, null),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(
                        10L, AccountType.RECEIVABLE, new BigDecimal("-1.00"), 10, "Taxa", 1L, 1L, null))
                .isInstanceOf(InvalidRecurringChargeAmountException.class);

        assertThat(existing.isActive()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdReturnsRecurringChargeRegardlessOfActiveState() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        RecurringCharge inactive = withId(
                new RecurringCharge(AccountType.RECEIVABLE, new BigDecimal("300.00"), 10, "Taxa", fund, party, null),
                10L);
        inactive.setDeactivatedAt(java.time.LocalDate.now());
        when(repository.findById(10L)).thenReturn(Optional.of(inactive));

        assertThat(service.findById(10L)).isEqualTo(inactive);
    }

    @Test
    void findByIdThrowsWhenRecurringChargeDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteMarksDeactivatedAtWithoutRemovingTheLine() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        RecurringCharge existing = withId(
                new RecurringCharge(AccountType.RECEIVABLE, new BigDecimal("300.00"), 10, "Taxa", fund, party, null),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(RecurringCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(10L);

        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDeactivatedAt()).isNotNull();
        verify(repository).save(existing);
    }

    @Test
    void findAllReturnsOnlyActiveOrderedByDescriptionThenId() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        RecurringCharge removed = withId(
                new RecurringCharge(AccountType.RECEIVABLE, BigDecimal.TEN, 10, "Zebra", fund, party, null), 1L);
        removed.setDeactivatedAt(java.time.LocalDate.now());
        RecurringCharge replacedByEdit = withId(
                new RecurringCharge(AccountType.RECEIVABLE, BigDecimal.TEN, 10, "Alpha", fund, party, null), 2L);
        replacedByEdit.setDeactivatedAt(java.time.LocalDate.now());
        RecurringCharge activeBravoHigherId = withId(
                new RecurringCharge(AccountType.RECEIVABLE, BigDecimal.TEN, 10, "Bravo", fund, party, null), 5L);
        RecurringCharge activeBravoLowerId = withId(
                new RecurringCharge(AccountType.RECEIVABLE, BigDecimal.TEN, 10, "Bravo", fund, party, null), 3L);
        RecurringCharge activeAlpha = withId(
                new RecurringCharge(AccountType.RECEIVABLE, BigDecimal.TEN, 10, "Alpha", fund, party, null), 4L);
        when(repository.findAll())
                .thenReturn(List.of(removed, replacedByEdit, activeBravoHigherId, activeBravoLowerId, activeAlpha));

        assertThat(service.findAll())
                .containsExactly(activeAlpha, activeBravoLowerId, activeBravoHigherId);
    }

    @Test
    void editingARecurringChargeDoesNotAffectAnAccountAlreadyGenerated() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = stubFund(1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        RecurringCharge original = withId(
                new RecurringCharge(AccountType.RECEIVABLE, new BigDecimal("300.00"), 10, "Taxa", fund, party, null),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.save(any(RecurringCharge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findAll()).thenReturn(List.of(original));

        AccountRepository accountRepository = org.mockito.Mockito.mock(AccountRepository.class);
        when(accountRepository.existsByRecurringChargeIdAndDueDateBetween(any(), any(), any()))
                .thenReturn(false);
        java.util.concurrent.atomic.AtomicReference<Account> savedAccount = new java.util.concurrent.atomic.AtomicReference<>();
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            savedAccount.set(account);
            return account;
        });
        RecurringChargeGenerationService generationService =
                new RecurringChargeGenerationService(repository, accountRepository);

        generationService.generatePendingAccounts();
        Account generated = savedAccount.get();

        assertThat(generated.getAmount()).isEqualByComparingTo("300.00");
        assertThat(generated.getDescription()).isEqualTo("Taxa");

        service.update(10L, AccountType.RECEIVABLE, new BigDecimal("330.00"), 10, "Taxa reajustada", 1L, 1L, null);

        assertThat(generated.getAmount()).isEqualByComparingTo("300.00");
        assertThat(generated.getDescription()).isEqualTo("Taxa");
    }

    private Fund stubFund(Long id) {
        Fund fund = withId(new Fund("Piscina", BigDecimal.ZERO), id);
        when(fundRepository.findById(id)).thenReturn(Optional.of(fund));
        return fund;
    }

    private Party withId(Party party, Long id) {
        ReflectionTestUtils.setField(party, "id", id);
        return party;
    }

    private Group withId(Group group, Long id) {
        ReflectionTestUtils.setField(group, "id", id);
        return group;
    }

    private Fund withId(Fund fund, Long id) {
        ReflectionTestUtils.setField(fund, "id", id);
        return fund;
    }

    private RecurringCharge withId(RecurringCharge recurringCharge, Long id) {
        ReflectionTestUtils.setField(recurringCharge, "id", id);
        return recurringCharge;
    }
}
