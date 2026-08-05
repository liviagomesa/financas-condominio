package com.financas.account.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financas.fund.domain.Fund;
import com.financas.fund.domain.FundRepository;
import com.financas.group.domain.EmptyGroupException;
import com.financas.group.domain.Group;
import com.financas.group.domain.GroupRepository;
import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import com.financas.recurringcharge.domain.RecurringCharge;
import com.financas.shared.exceptions.BadRequestException;
import com.financas.shared.exceptions.NotFoundException;

@Service
public class AccountService {

    private static final Pattern PART_SUFFIX_PATTERN = Pattern.compile("^(.*) - parte (\\d+)$");

    private final AccountRepository repository;
    private final PartyRepository partyRepository;
    private final GroupRepository groupRepository;
    private final FundRepository fundRepository;

    public AccountService(
            AccountRepository repository,
            PartyRepository partyRepository,
            GroupRepository groupRepository,
            FundRepository fundRepository) {
        this.repository = repository;
        this.partyRepository = partyRepository;
        this.groupRepository = groupRepository;
        this.fundRepository = fundRepository;
    }

    public Account create(
            AccountType type,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Long fundId,
            Long partyId,
            LocalDate paymentDate,
            String observations) {
        validateNonNegativeAmount(amount);
        Fund fund = findFundOrThrow(fundId);
        Party party = resolveParty(partyId);
        return repository.save(
                new Account(type, amount, dueDate, description, fund, party, paymentDate, observations));
    }

    public Account createFromRecurringCharge(RecurringCharge recurringCharge, LocalDate dueDate) {
        validateNonNegativeAmount(recurringCharge.getAmount());
        Account account = new Account(
                recurringCharge.getType(),
                recurringCharge.getAmount(),
                dueDate,
                recurringCharge.getDescription(),
                recurringCharge.getFund(),
                recurringCharge.getParty(),
                null,
                recurringCharge.getObservations());
        account.setRecurringCharge(recurringCharge);
        return repository.save(account);
    }

    @Transactional
    public List<Account> createForGroup(
            AccountType type,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Long fundId,
            Long groupId,
            LocalDate paymentDate,
            String observations) {
        validateNonNegativeAmount(amount);
        Fund fund = findFundOrThrow(fundId);
        Group group = findGroupOrThrow(groupId);
        if (group.getMembers().isEmpty()) {
            throw new EmptyGroupException();
        }
        return group.getMembers().stream()
                .map(party -> repository.save(
                        new Account(type, amount, dueDate, description, fund, party, paymentDate, observations)))
                .toList();
    }

    /**
     * Dado o volume pequeno de registros (poucas dezenas), a filtragem é feita em memória, sem
     * necessidade de índices ou consultas otimizadas dedicadas.
     */
    public List<Account> findAll(
            Long partyId,
            Long fundId,
            AccountType type,
            Boolean paid,
            Boolean overdue,
            String dueYearMonth,
            String paymentYearMonth) {
        List<Account> accounts;
        if (partyId != null) {
            findPartyOrThrow(partyId);
            accounts = repository.findByPartyId(partyId);
        } else {
            accounts = repository.findAll();
        }

        if (fundId != null) {
            accounts =
                    accounts.stream().filter(a -> a.getFund().getId().equals(fundId)).toList();
        }
        if (type != null) {
            accounts = accounts.stream().filter(a -> a.getType() == type).toList();
        }
        if (paid != null) {
            accounts = accounts.stream().filter(a -> a.isPaid() == paid).toList();
        }
        if (Boolean.TRUE.equals(overdue)) {
            LocalDate today = LocalDate.now();
            accounts = accounts.stream()
                    .filter(a -> !a.isPaid() && a.getDueDate().isBefore(today))
                    .toList();
        }
        if (dueYearMonth != null) {
            YearMonth month = parseYearMonth(dueYearMonth, "vencimento");
            accounts = accounts.stream()
                    .filter(a -> YearMonth.from(a.getDueDate()).equals(month))
                    .toList();
        }
        if (paymentYearMonth != null) {
            YearMonth month = parseYearMonth(paymentYearMonth, "pagamento");
            accounts = accounts.stream()
                    .filter(a -> a.isPaid() && YearMonth.from(a.getPaymentDate()).equals(month))
                    .toList();
        }
        return accounts.stream()
                .sorted(Comparator.comparing(Account::getDueDate, Comparator.reverseOrder())
                        .thenComparing(Account::getDescription)
                        .thenComparing(Account::getId, Comparator.reverseOrder()))
                .toList();
    }

    public Account findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Conta não encontrada."));
    }

    public Account update(
            Long id,
            AccountType type,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Long fundId,
            Long partyId,
            LocalDate paymentDate,
            String observations) {
        Account account = findById(id);
        if (account.getType() != type) {
            throw new AccountTypeChangeNotAllowedException();
        }
        validateNonNegativeAmount(amount);
        Fund fund = findFundOrThrow(fundId);
        Party party = resolveParty(partyId);
        account.setAmount(amount);
        account.setDueDate(dueDate);
        account.setDescription(description);
        account.setFund(fund);
        account.setParty(party);
        account.setPaymentDate(paymentDate);
        account.setObservations(observations);
        return repository.save(account);
    }

    @Transactional
    public Account registerPayment(Long id, LocalDate paymentDate, BigDecimal paidAmount) {
        Account account = findById(id);
        BigDecimal amountDue = account.getAmount();
        BigDecimal effectivePaidAmount = paidAmount == null ? amountDue : paidAmount;
        int comparison = effectivePaidAmount.compareTo(amountDue);

        if (comparison == 0) {
            account.setPaymentDate(paymentDate);
            return repository.save(account);
        }

        if (comparison < 0) {
            Matcher matcher = PART_SUFFIX_PATTERN.matcher(account.getDescription());
            String baseDescription;
            int currentPart;
            if (matcher.matches()) {
                baseDescription = matcher.group(1);
                currentPart = Integer.parseInt(matcher.group(2));
            } else {
                baseDescription = account.getDescription();
                currentPart = 1;
                account.setDescription(baseDescription + " - parte 1");
            }
            account.setAmount(effectivePaidAmount);
            account.setPaymentDate(paymentDate);
            Account paidAccount = repository.save(account);

            Account remaining = new Account(
                    paidAccount.getType(),
                    amountDue.subtract(effectivePaidAmount),
                    paidAccount.getDueDate(),
                    baseDescription + " - parte " + (currentPart + 1),
                    paidAccount.getFund(),
                    paidAccount.getParty(),
                    null,
                    paidAccount.getObservations());
            repository.save(remaining);
            return paidAccount;
        }

        BigDecimal overpaidAmount = effectivePaidAmount.subtract(amountDue);
        String note = "pago R$" + overpaidAmount.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',')
                + " a mais";
        String existingObservations = account.getObservations();
        String updatedObservations = (existingObservations == null || existingObservations.isBlank())
                ? note
                : existingObservations + "\n" + note;
        account.setAmount(effectivePaidAmount);
        account.setPaymentDate(paymentDate);
        account.setObservations(updatedObservations);
        return repository.save(account);
    }

    public Account duplicate(Long id, boolean zeroAmount) {
        Account original = findById(id);
        return repository.save(new Account(
                original.getType(),
                zeroAmount ? BigDecimal.ZERO : original.getAmount(),
                original.getDueDate().plusMonths(1),
                original.getDescription(),
                original.getFund(),
                original.getParty(),
                null,
                original.getObservations()));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Conta não encontrada.");
        }
        repository.deleteById(id);
    }

    private void validateNonNegativeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAccountAmountException();
        }
    }

    private Party resolveParty(Long partyId) {
        if (partyId == null) {
            throw new BadRequestException("A parte é obrigatória.");
        }
        return findPartyOrThrow(partyId);
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository
                .findById(partyId)
                .orElseThrow(() -> new NotFoundException(
                        "Parte não encontrada. Cadastre uma parte antes de lançar uma conta."));
    }

    private Group findGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Grupo não encontrado."));
    }

    private Fund findFundOrThrow(Long fundId) {
        return fundRepository
                .findById(fundId)
                .orElseThrow(() -> new NotFoundException(
                        "Fundo não encontrado. Cadastre um fundo antes de lançar uma conta."));
    }

    private YearMonth parseYearMonth(String value, String fieldLabel) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Mês/ano de " + fieldLabel + " inválido. Use o formato AAAA-MM.");
        }
    }
}
