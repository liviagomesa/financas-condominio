package com.financas.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.fund.domain.Fund;
import com.financas.fund.domain.FundRepository;
import com.financas.group.domain.Group;
import com.financas.group.domain.GroupRepository;
import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class AccountServiceTest {

    @Mock
    private AccountRepository repository;

    @Mock
    private PartyRepository partyRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private FundRepository fundRepository;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(repository, partyRepository, groupRepository, fundRepository);
    }

    @Test
    void createsReceivableForAnyParty() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        stubFund(1L);
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(
                AccountType.RECEIVABLE,
                new BigDecimal("350.00"),
                LocalDate.of(2026, 8, 10),
                "Taxa condominial - Agosto/2026",
                1L,
                true,
                1L,
                null,
                null);

        assertThat(created.getType()).isEqualTo(AccountType.RECEIVABLE);
        assertThat(created.getAmount()).isEqualByComparingTo("350.00");
        assertThat(created.getParty()).isEqualTo(party);
        assertThat(created.isPaid()).isFalse();
    }

    @Test
    void createsPayableForTheSamePartyUsedAsReceivable() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        stubFund(1L);
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(
                AccountType.PAYABLE,
                new BigDecimal("400.00"),
                LocalDate.of(2026, 8, 5),
                "Reembolso",
                1L,
                false,
                1L,
                null,
                "Disse que vai pagar mês que vem");

        assertThat(created.getType()).isEqualTo(AccountType.PAYABLE);
        assertThat(created.getParty()).isEqualTo(party);
        assertThat(created.getObservations()).isEqualTo("Disse que vai pagar mês que vem");
    }

    @Test
    void createAcceptsZeroAmount() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        stubFund(1L);
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = service.create(
                AccountType.RECEIVABLE, BigDecimal.ZERO, LocalDate.now(), "Taxa", 1L, true, 1L, null, null);

        assertThat(created.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsCreateWhenAmountIsNegative() {
        assertThatThrownBy(() -> service.create(
                        AccountType.RECEIVABLE,
                        new BigDecimal("-10.00"),
                        LocalDate.now(),
                        "Taxa",
                        1L,
                        true,
                        1L,
                        null,
                        null))
                .isInstanceOf(InvalidAccountAmountException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsCreateWhenFundDoesNotExist() {
        when(fundRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                        AccountType.RECEIVABLE,
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        999L,
                        true,
                        1L,
                        null,
                        null))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsCreateWithoutParty() {
        stubFund(1L);

        assertThatThrownBy(() -> service.create(
                        AccountType.RECEIVABLE, new BigDecimal("350.00"), LocalDate.now(), "Taxa", 1L, true, null,
                        null, null))
                .isInstanceOf(com.financas.shared.exceptions.BadRequestException.class);
    }

    @Test
    void updateRejectsTypeChange() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Account existing = withId(receivable(party, new BigDecimal("350.00"), LocalDate.now()), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(
                        10L,
                        AccountType.PAYABLE,
                        new BigDecimal("350.00"),
                        LocalDate.now(),
                        "Taxa",
                        1L,
                        true,
                        1L,
                        null,
                        null))
                .isInstanceOf(AccountTypeChangeNotAllowedException.class);
    }

    @Test
    void createForGroupCreatesOneAccountPerMemberWithExplicitType() {
        Party partyA = withId(new Party("Bloco A - 101", null), 1L);
        Party partyB = withId(new Party("Bloco A - 102", null), 2L);
        Group group = withId(new Group("Bloco A", Set.of(partyA, partyB)), 1L);
        stubFund(1L);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Account> created = service.createForGroup(
                AccountType.RECEIVABLE, new BigDecimal("350.00"), LocalDate.of(2026, 8, 10), "Taxa", 1L, true, 1L,
                null, null);

        assertThat(created).hasSize(2);
        assertThat(created).allSatisfy(account -> assertThat(account.getType()).isEqualTo(AccountType.RECEIVABLE));
        assertThat(created).extracting(Account::getParty).containsExactlyInAnyOrder(partyA, partyB);
    }

    @Test
    void rejectsCreateForGroupWhenGroupIsEmpty() {
        Group group = withId(new Group("Bloco A", Set.of()), 1L);
        stubFund(1L);
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.createForGroup(
                        AccountType.RECEIVABLE, new BigDecimal("350.00"), LocalDate.now(), "Taxa", 1L, true, 1L, null,
                        null))
                .isInstanceOf(EmptyGroupException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void findAllFiltersByTypeAlone() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Account receivableAccount = withId(receivable(party, new BigDecimal("350.00"), LocalDate.now()), 1L);
        Account payableAccount = withId(payable(party, new BigDecimal("400.00"), LocalDate.now()), 2L);
        when(repository.findAll()).thenReturn(List.of(receivableAccount, payableAccount));

        assertThat(service.findAll(null, null, AccountType.PAYABLE, null, null, null, null))
                .containsExactly(payableAccount);
        assertThat(service.findAll(null, null, AccountType.RECEIVABLE, null, null, null, null))
                .containsExactly(receivableAccount);
    }

    @Test
    void findAllCombinesTypeWithPartyId() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        Account payableAccount = withId(payable(party, new BigDecimal("400.00"), LocalDate.now()), 2L);
        when(repository.findByPartyId(1L)).thenReturn(List.of(payableAccount));

        assertThat(service.findAll(1L, null, AccountType.PAYABLE, null, null, null, null))
                .containsExactly(payableAccount);
    }

    @Test
    void findAllCombinesFundIdWithOtherFilters() {
        Fund fundA = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        Fund fundB = withId(new Fund("Jardim", BigDecimal.ZERO), 2L);
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Account inFundA = withId(receivableWithFund(party, fundA, new BigDecimal("350.00")), 1L);
        Account inFundB = withId(receivableWithFund(party, fundB, new BigDecimal("100.00")), 2L);
        when(repository.findAll()).thenReturn(List.of(inFundA, inFundB));

        assertThat(service.findAll(null, 1L, null, null, null, null, null)).containsExactly(inFundA);
    }

    @Test
    void findAllCombinesTypeWithPartyIdPaidAndOverdue() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        when(partyRepository.findById(1L)).thenReturn(Optional.of(party));
        Account overdueReceivable =
                withId(receivable(party, new BigDecimal("350.00"), LocalDate.now().minusDays(1)), 1L);
        Account paidReceivable = withId(
                receivableWithPayment(party, new BigDecimal("350.00"), LocalDate.now().minusDays(1), LocalDate.now()),
                2L);
        when(repository.findByPartyId(1L)).thenReturn(List.of(overdueReceivable, paidReceivable));

        assertThat(service.findAll(1L, null, AccountType.RECEIVABLE, false, true, null, null))
                .containsExactly(overdueReceivable);
    }

    @Test
    void findAllThrowsWhenPartyIdFilterDoesNotExist() {
        when(partyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAll(999L, null, null, null, null, null, null))
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
                        1L,
                        true,
                        1L,
                        null,
                        null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void registerPaymentMarksPayableAccountAsPaid() {
        Party party = withId(new Party("Fornecedor", null), 1L);
        Account existing = withId(payable(party, new BigDecimal("400.00"), LocalDate.now()), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account paid = service.registerPayment(10L, LocalDate.of(2026, 8, 15));

        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void overdueFilterIncludesPendingPayableAndExcludesPaidPayable() {
        Party party = withId(new Party("Fornecedor", null), 1L);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Account overduePending = withId(payable(party, new BigDecimal("400.00"), yesterday), 1L);
        Account overduePaid =
                withId(payableWithPayment(party, new BigDecimal("400.00"), yesterday, LocalDate.now()), 2L);
        when(repository.findAll()).thenReturn(List.of(overduePending, overduePaid));

        assertThat(service.findAll(null, null, null, null, true, null, null)).containsExactly(overduePending);
    }

    @Test
    void duplicateCopiesFieldsAndAdvancesDueDateByOneMonth() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Account original = withId(
                new Account(
                        AccountType.RECEIVABLE,
                        new BigDecimal("350.00"),
                        LocalDate.of(2026, 3, 10),
                        "Taxa condominial",
                        testFund(),
                        true,
                        party,
                        LocalDate.of(2026, 3, 5),
                        "Observação"),
                10L);
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account copy = service.duplicate(10L, false);

        assertThat(copy.getType()).isEqualTo(original.getType());
        assertThat(copy.getDescription()).isEqualTo(original.getDescription());
        assertThat(copy.getFund()).isEqualTo(original.getFund());
        assertThat(copy.isRecurring()).isEqualTo(original.isRecurring());
        assertThat(copy.getParty()).isEqualTo(original.getParty());
        assertThat(copy.getObservations()).isEqualTo(original.getObservations());
        assertThat(copy.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(copy.getAmount()).isEqualByComparingTo("350.00");
        assertThat(copy.getPaymentDate()).isNull();
        assertThat(original.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(original.getPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void duplicateHandlesShortMonthOverflow() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Account original =
                withId(receivable(party, new BigDecimal("350.00"), LocalDate.of(2026, 1, 31)), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account copy = service.duplicate(10L, false);

        assertThat(copy.getDueDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void duplicateAlwaysClearsPaymentDateEvenWhenOriginalIsPaid() {
        Party party = withId(new Party("Fornecedor", null), 1L);
        Account original = withId(
                payableWithPayment(party, new BigDecimal("400.00"), LocalDate.now(), LocalDate.now()), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account copy = service.duplicate(10L, false);

        assertThat(copy.getPaymentDate()).isNull();
        assertThat(copy.isPaid()).isFalse();
    }

    @Test
    void duplicateWithZeroAmountTrueZeroesTheAmount() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Account original = withId(receivable(party, new BigDecimal("350.00"), LocalDate.now()), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account copy = service.duplicate(10L, true);

        assertThat(copy.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void duplicateWithZeroAmountFalseKeepsOriginalAmount() {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Account original = withId(receivable(party, new BigDecimal("350.00"), LocalDate.now()), 10L);
        when(repository.findById(10L)).thenReturn(Optional.of(original));
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account copy = service.duplicate(10L, false);

        assertThat(copy.getAmount()).isEqualByComparingTo("350.00");
    }

    @Test
    void duplicateThrowsWhenAccountDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.duplicate(999L, false)).isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
    }

    private Account receivable(Party party, BigDecimal amount, LocalDate dueDate) {
        return new Account(AccountType.RECEIVABLE, amount, dueDate, "Taxa", testFund(), true, party, null, null);
    }

    private Account receivableWithFund(Party party, Fund fund, BigDecimal amount) {
        return new Account(AccountType.RECEIVABLE, amount, LocalDate.now(), "Taxa", fund, true, party, null, null);
    }

    private Account receivableWithPayment(Party party, BigDecimal amount, LocalDate dueDate, LocalDate paymentDate) {
        return new Account(
                AccountType.RECEIVABLE, amount, dueDate, "Taxa", testFund(), true, party, paymentDate, null);
    }

    private Account payable(Party party, BigDecimal amount, LocalDate dueDate) {
        return new Account(AccountType.PAYABLE, amount, dueDate, "Limpeza", testFund(), false, party, null, null);
    }

    private Account payableWithPayment(Party party, BigDecimal amount, LocalDate dueDate, LocalDate paymentDate) {
        return new Account(
                AccountType.PAYABLE, amount, dueDate, "Limpeza", testFund(), false, party, paymentDate, null);
    }

    private Fund testFund() {
        return withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
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

    private Account withId(Account account, Long id) {
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Fund withId(Fund fund, Long id) {
        ReflectionTestUtils.setField(fund, "id", id);
        return fund;
    }
}
