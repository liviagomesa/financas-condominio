package com.financas.recurringcharge.domain;

import java.time.LocalDate;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.financas.account.domain.AccountRepository;
import com.financas.account.domain.AccountService;

/**
 * Isolamento por cobrança (FR-016) exige que este orquestrador NÃO seja {@code @Transactional}
 * — ver research.md/plan.md Complexity Tracking (divergência deliberada do Princípio II).
 */
@Service
public class RecurringChargeGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RecurringChargeGenerationService.class);

    private final RecurringChargeRepository recurringChargeRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public RecurringChargeGenerationService(
            RecurringChargeRepository recurringChargeRepository,
            AccountRepository accountRepository,
            AccountService accountService) {
        this.recurringChargeRepository = recurringChargeRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    @Scheduled(cron = "0 0 6 25 * *", zone = "America/Sao_Paulo") // gatilho 1
    @EventListener(ApplicationReadyEvent.class) // gatilho 2: aplicação sobe
    public void generatePendingAccounts() {
        YearMonth targetMonth = resolveMostRecentDueTargetMonth(LocalDate.now());
        LocalDate start = targetMonth.atDay(1);
        LocalDate end = targetMonth.atEndOfMonth();

        for (RecurringCharge charge : recurringChargeRepository.findAll()) {
            if (!charge.isActive()) {
                continue;
            }
            // se já existe uma Account vinculada a esta cobrança com vencimento dentro do mês-alvo, não gera de novo
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
                // registra a exceção com stack trace no log para consultar a causa depois, se algum bug surgir em prod
                log.error("Failed to generate account for recurring charge {}", charge.getId(), e);
                charge.setLastGenerationFailed(true);
                recurringChargeRepository.save(charge);
            }
        }
    }

    private void generateOne(RecurringCharge charge, YearMonth targetMonth) {
        int dueDay = Math.min(charge.getDueDay(), targetMonth.lengthOfMonth());
        LocalDate dueDate = targetMonth.atDay(dueDay);
        accountService.createFromRecurringCharge(charge, dueDate);

        charge.setLastGenerationFailed(false);
        recurringChargeRepository.save(charge);
    }

    private YearMonth resolveMostRecentDueTargetMonth(LocalDate reference) {
        return reference.getDayOfMonth() >= 25 ? YearMonth.from(reference).plusMonths(1) : YearMonth.from(reference);
    }
}
