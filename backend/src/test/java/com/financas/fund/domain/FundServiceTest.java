package com.financas.fund.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountRepository;
import com.financas.account.domain.AccountType;
import com.financas.shared.exceptions.NotFoundException;
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
class FundServiceTest {

    @Mock
    private FundRepository repository;

    @Mock
    private AccountRepository accountRepository;

    private FundService service;

    @BeforeEach
    void setUp() {
        service = new FundService(repository, accountRepository);
    }

    @Test
    void createsFundWhenNameIsNotDuplicated() {
        when(repository.findByNormalizedName("Piscina")).thenReturn(Optional.empty());
        when(repository.save(any(Fund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fund created = service.create("Piscina", new BigDecimal("500.00"));

        assertThat(created.getName()).isEqualTo("Piscina");
        assertThat(created.getInitialBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void rejectsCreateWhenNameIsDuplicatedIgnoringCaseAndWhitespace() {
        Fund existing = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        when(repository.findByNormalizedName(" piscina ")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(" piscina ", BigDecimal.ZERO))
                .isInstanceOf(DuplicateFundException.class);
    }

    @Test
    void allowsUpdateKeepingTheSameNameOnTheSameFund() {
        Fund existing = withId(new Fund("Piscina", new BigDecimal("500.00")), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.findByNormalizedName("Piscina")).thenReturn(Optional.of(existing));
        when(repository.save(any(Fund.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fund updated = service.update(1L, "Piscina", new BigDecimal("600.00"));

        assertThat(updated.getInitialBalance()).isEqualByComparingTo("600.00");
    }

    @Test
    void rejectsUpdateWhenNameBelongsToAnotherFund() {
        Fund current = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        Fund other = withId(new Fund("Jardim", BigDecimal.ZERO), 2L);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.findByNormalizedName("Jardim")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L, "Jardim", BigDecimal.ZERO))
                .isInstanceOf(DuplicateFundException.class);
    }

    @Test
    void findByIdThrowsWhenFundDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesFundWithoutAccounts() {
        Fund existing = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByFundId(1L)).thenReturn(false);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteRejectsFundWithAccountsLinked() {
        Fund existing = withId(new Fund("Piscina", BigDecimal.ZERO), 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.existsByFundId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(FundHasAccountsException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void calculateRealBalanceSumsPaidReceivablesAndSubtractsPaidPayables() {
        Fund fund = withId(new Fund("Piscina", new BigDecimal("500.00")), 1L);
        Account paidReceivable = account(AccountType.RECEIVABLE, new BigDecimal("350.00"), LocalDate.now());
        Account unpaidReceivable = account(AccountType.RECEIVABLE, new BigDecimal("999.00"), null);
        Account paidPayable = account(AccountType.PAYABLE, new BigDecimal("100.00"), LocalDate.now());
        Account unpaidPayable = account(AccountType.PAYABLE, new BigDecimal("999.00"), null);
        when(accountRepository.findByFundId(1L))
                .thenReturn(List.of(paidReceivable, unpaidReceivable, paidPayable, unpaidPayable));

        BigDecimal balance = service.calculateRealBalance(fund);

        assertThat(balance).isEqualByComparingTo("750.00");
    }

    @Test
    void calculateRealBalanceAllowsNegativeResultWithoutThrowing() {
        Fund fund = withId(new Fund("Jardim", BigDecimal.ZERO), 1L);
        Account paidPayable = account(AccountType.PAYABLE, new BigDecimal("50.00"), LocalDate.now());
        when(accountRepository.findByFundId(1L)).thenReturn(List.of(paidPayable));

        BigDecimal balance = service.calculateRealBalance(fund);

        assertThat(balance).isEqualByComparingTo("-50.00");
    }

    private Account account(AccountType type, BigDecimal amount, LocalDate paymentDate) {
        return new Account(type, amount, LocalDate.now(), "Descrição", null, false, null, null, paymentDate, null);
    }

    private Fund withId(Fund fund, Long id) {
        ReflectionTestUtils.setField(fund, "id", id);
        return fund;
    }
}
