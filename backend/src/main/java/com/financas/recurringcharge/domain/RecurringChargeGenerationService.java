package com.financas.recurringcharge.domain;

import com.financas.account.domain.Account;
import com.financas.account.domain.AccountRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Isolamento por cobrança (FR-016) exige que este orquestrador NÃO seja {@code @Transactional}
 * — ver research.md/plan.md Complexity Tracking (divergência deliberada do Princípio II).
 */
@Service
public class RecurringChargeGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RecurringChargeGenerationService.class);

    private final RecurringChargeRepository recurringChargeRepository;
    private final AccountRepository accountRepository;

    public RecurringChargeGenerationService(
            RecurringChargeRepository recurringChargeRepository, AccountRepository accountRepository) {
        this.recurringChargeRepository = recurringChargeRepository;
        this.accountRepository = accountRepository;
    }

    @Scheduled(cron = "0 0 6 25 * *", zone = "America/Sao_Paulo")
    @EventListener(ApplicationReadyEvent.class)
    public void generatePendingAccounts() {
        YearMonth targetMonth = resolveMostRecentDueTargetMonth(LocalDate.now());
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();

        for (RecurringCharge charge : recurringChargeRepository.findAll()) {
            if (!charge.isActive()) {
                continue;
            }
            if (accountRepository.existsByRecurringChargeIdAndDueDateBetween(charge.getId(), start, end)) {
                if (charge.isLastGenerationFailed()) {
                    charge.setLastGenerationFailed(false);
                    recurringChargeRepository.save(charge);
                }
                continue;
            }
            try {
                generateOne(charge, targetMonth);
            } catch (RuntimeException e) {
                log.error("Failed to generate account for recurring charge {}", charge.getId(), e);
                charge.setLastGenerationFailed(true);
                recurringChargeRepository.save(charge);
            }
        }
    }

    private void generateOne(RecurringCharge charge, YearMonth targetMonth) {
        int dueDay = Math.min(charge.getDueDay(), targetMonth.lengthOfMonth());
        LocalDate dueDate = targetMonth.atDay(dueDay);
        Account account = new Account(
                charge.getType(),
                charge.getAmount(),
                dueDate,
                charge.getDescription(),
                charge.getFund(),
                charge.getParty(),
                null,
                charge.getObservations());
        account.setRecurringCharge(charge);
        accountRepository.save(account);

        charge.setLastGenerationFailed(false);
        recurringChargeRepository.save(charge);
    }

    private YearMonth resolveMostRecentDueTargetMonth(LocalDate reference) {
        return reference.getDayOfMonth() >= 25 ? YearMonth.from(reference).plusMonths(1) : YearMonth.from(reference);
    }
}
