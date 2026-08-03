package com.financas.recurringcharge.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountRepository;
import com.financas.account.domain.AccountType;
import com.financas.fund.domain.Fund;
import com.financas.party.domain.Party;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecurringChargeGenerationServiceTest {

    @Mock
    private RecurringChargeRepository recurringChargeRepository;

    @Mock
    private AccountRepository accountRepository;

    private RecurringChargeGenerationService service;

    @BeforeEach
    void setUp() {
        service = new RecurringChargeGenerationService(recurringChargeRepository, accountRepository);
    }

    @Test
    void generatesOneAccountPerActiveRecurringChargeReferencingTheTemplate() {
        RecurringCharge chargeA = withId(recurringCharge(10, "Taxa condominial"), 1L);
        RecurringCharge chargeB = withId(recurringCharge(15, "Taxa de limpeza"), 2L);
        when(recurringChargeRepository.findAll()).thenReturn(List.of(chargeA, chargeB));
        when(accountRepository.existsByRecurringChargeIdAndDueDateBetween(any(), any(), any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recurringChargeRepository.save(any(RecurringCharge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generatePendingAccounts();

        YearMonth targetMonth = resolveTargetMonth();
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository, times(2)).save(captor.capture());
        List<Account> saved = captor.getAllValues();
        assertThat(saved).extracting(Account::getRecurringCharge).containsExactly(chargeA, chargeB);
        assertThat(saved).allSatisfy(
                account -> assertThat(YearMonth.from(account.getDueDate())).isEqualTo(targetMonth));
        assertThat(chargeA.isLastGenerationFailed()).isFalse();
        assertThat(chargeB.isLastGenerationFailed()).isFalse();
    }

    @Test
    void secondCallInTheSameCycleDoesNotDuplicateAccounts() {
        RecurringCharge charge = withId(recurringCharge(10, "Taxa condominial"), 1L);
        when(recurringChargeRepository.findAll()).thenReturn(List.of(charge));
        when(accountRepository.existsByRecurringChargeIdAndDueDateBetween(any(), any(), any())).thenReturn(true);

        service.generatePendingAccounts();

        verify(accountRepository, never()).save(any());
    }

    @Test
    void adjustsDueDayToTheLastDayOfAShorterTargetMonth() {
        RecurringCharge charge = withId(recurringCharge(31, "Taxa condominial"), 1L);
        when(recurringChargeRepository.findAll()).thenReturn(List.of(charge));
        when(accountRepository.existsByRecurringChargeIdAndDueDateBetween(any(), any(), any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.generatePendingAccounts();

        YearMonth targetMonth = resolveTargetMonth();
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getDueDate()).isEqualTo(targetMonth.atEndOfMonth());
    }

    @Test
    void doesNothingWhenThereAreNoActiveRecurringCharges() {
        when(recurringChargeRepository.findAll()).thenReturn(List.of());

        service.generatePendingAccounts();

        verify(accountRepository, never()).save(any());
    }

    @Test
    void inactiveRecurringChargesAreSkipped() {
        RecurringCharge inactive = withId(recurringCharge(10, "Taxa antiga"), 1L);
        inactive.setDeactivatedAt(LocalDate.now());
        when(recurringChargeRepository.findAll()).thenReturn(List.of(inactive));

        service.generatePendingAccounts();

        verify(accountRepository, never()).save(any());
        verify(accountRepository, never()).existsByRecurringChargeIdAndDueDateBetween(any(), any(), any());
    }

    @Test
    void isolatesFailureOfOneRecurringChargeFromTheOthers() {
        RecurringCharge failing = withId(recurringCharge(10, "Falha"), 1L);
        RecurringCharge succeeding = withId(recurringCharge(15, "Sucesso"), 2L);
        when(recurringChargeRepository.findAll()).thenReturn(List.of(failing, succeeding));
        when(accountRepository.existsByRecurringChargeIdAndDueDateBetween(any(), any(), any())).thenReturn(false);
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new RuntimeException("fundo removido"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(recurringChargeRepository.save(any(RecurringCharge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generatePendingAccounts();

        assertThat(failing.isLastGenerationFailed()).isTrue();
        assertThat(succeeding.isLastGenerationFailed()).isFalse();
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void clearsFailedFlagWhenAccountForTheTargetMonthAlreadyExists() {
        RecurringCharge charge = withId(recurringCharge(10, "Taxa condominial"), 1L);
        charge.setLastGenerationFailed(true);
        when(recurringChargeRepository.findAll()).thenReturn(List.of(charge));
        when(accountRepository.existsByRecurringChargeIdAndDueDateBetween(any(), any(), any())).thenReturn(true);
        when(recurringChargeRepository.save(any(RecurringCharge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generatePendingAccounts();

        assertThat(charge.isLastGenerationFailed()).isFalse();
        verify(recurringChargeRepository).save(charge);
    }

    @Test
    void resolveMostRecentDueTargetMonthReturnsCurrentMonthBeforeDay25() {
        YearMonth result = ReflectionTestUtils.invokeMethod(
                service, "resolveMostRecentDueTargetMonth", LocalDate.of(2026, 8, 24));

        assertThat(result).isEqualTo(YearMonth.of(2026, 8));
    }

    @Test
    void resolveMostRecentDueTargetMonthReturnsNextMonthOnDay25() {
        YearMonth result = ReflectionTestUtils.invokeMethod(
                service, "resolveMostRecentDueTargetMonth", LocalDate.of(2026, 8, 25));

        assertThat(result).isEqualTo(YearMonth.of(2026, 9));
    }

    @Test
    void resolveMostRecentDueTargetMonthReturnsNextMonthAfterDay25() {
        YearMonth result = ReflectionTestUtils.invokeMethod(
                service, "resolveMostRecentDueTargetMonth", LocalDate.of(2026, 8, 30));

        assertThat(result).isEqualTo(YearMonth.of(2026, 9));
    }

    private YearMonth resolveTargetMonth() {
        LocalDate now = LocalDate.now();
        return now.getDayOfMonth() >= 25 ? YearMonth.from(now).plusMonths(1) : YearMonth.from(now);
    }

    private RecurringCharge recurringCharge(int dueDay, String description) {
        Party party = withId(new Party("Bloco A - 101", null), 1L);
        Fund fund = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        return new RecurringCharge(AccountType.RECEIVABLE, new BigDecimal("350.00"), dueDay, description, fund, party, null);
    }

    private Party withId(Party party, Long id) {
        ReflectionTestUtils.setField(party, "id", id);
        return party;
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
