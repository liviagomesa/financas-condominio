package com.financas.account.domain;

import com.financas.fund.domain.Fund;
import com.financas.fund.domain.FundRepository;
import com.financas.group.domain.Group;
import com.financas.group.domain.GroupRepository;
import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import com.financas.shared.exceptions.BadRequestException;
import com.financas.shared.exceptions.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

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
            boolean recurring,
            Long partyId,
            LocalDate paymentDate,
            String observations) {
        validateNonNegativeAmount(amount);
        Fund fund = findFundOrThrow(fundId);
        Party party = resolveParty(partyId);
        return repository.save(
                new Account(type, amount, dueDate, description, fund, recurring, party, paymentDate, observations));
    }

    @Transactional
    public List<Account> createForGroup(
            AccountType type,
            BigDecimal amount,
            LocalDate dueDate,
            String description,
            Long fundId,
            boolean recurring,
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
                .map(party -> repository.save(new Account(
                        type, amount, dueDate, description, fund, recurring, party, paymentDate, observations)))
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
        return accounts;
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
            boolean recurring,
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
        account.setRecurring(recurring);
        account.setParty(party);
        account.setPaymentDate(paymentDate);
        account.setObservations(observations);
        return repository.save(account);
    }

    public Account registerPayment(Long id, LocalDate paymentDate) {
        Account account = findById(id);
        account.setPaymentDate(paymentDate);
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
                original.isRecurring(),
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
