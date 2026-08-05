package com.financas.recurringcharge.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financas.account.domain.AccountType;
import com.financas.fund.domain.Fund;
import com.financas.fund.domain.FundRepository;
import com.financas.group.domain.EmptyGroupException;
import com.financas.group.domain.Group;
import com.financas.group.domain.GroupRepository;
import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import com.financas.shared.exceptions.NotFoundException;

@Service
public class RecurringChargeService {

    private final RecurringChargeRepository repository;
    private final PartyRepository partyRepository;
    private final GroupRepository groupRepository;
    private final FundRepository fundRepository;

    public RecurringChargeService(
            RecurringChargeRepository repository,
            PartyRepository partyRepository,
            GroupRepository groupRepository,
            FundRepository fundRepository) {
        this.repository = repository;
        this.partyRepository = partyRepository;
        this.groupRepository = groupRepository;
        this.fundRepository = fundRepository;
    }

    public RecurringCharge create(
            AccountType type,
            BigDecimal amount,
            Integer dueDay,
            String description,
            Long fundId,
            Long partyId,
            String observations) {
        validateNonNegativeAmount(amount);
        Fund fund = findFundOrThrow(fundId);
        Party party = findPartyOrThrow(partyId);
        return repository.save(new RecurringCharge(type, amount, dueDay, description, fund, party, observations));
    }

    @Transactional
    public List<RecurringCharge> createForGroup(
            AccountType type,
            BigDecimal amount,
            Integer dueDay,
            String description,
            Long fundId,
            Long groupId,
            String observations) {
        validateNonNegativeAmount(amount);
        Fund fund = findFundOrThrow(fundId);
        Group group = findGroupOrThrow(groupId);
        if (group.getMembers().isEmpty()) {
            throw new EmptyGroupException();
        }
        return group.getMembers().stream()
                .map(party ->
                        repository.save(new RecurringCharge(type, amount, dueDay, description, fund, party, observations)))
                .toList();
    }

    public List<RecurringCharge> findAll() {
        return repository.findAll().stream()
                .filter(RecurringCharge::isActive)
                .sorted(Comparator.comparing(RecurringCharge::getDescription).thenComparing(RecurringCharge::getId))
                .toList();
    }

    public RecurringCharge findById(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Cobrança recorrente não encontrada."));
    }

    @Transactional
    public RecurringCharge update(
            Long id,
            AccountType type,
            BigDecimal amount,
            Integer dueDay,
            String description,
            Long fundId,
            Long partyId,
            String observations) {
        RecurringCharge existing = findById(id);
        if (existing.getType() != type) {
            throw new RecurringChargeTypeChangeNotAllowedException();
        }
        validateNonNegativeAmount(amount);
        Fund fund = findFundOrThrow(fundId);
        Party party = findPartyOrThrow(partyId);

        existing.setDeactivatedAt(LocalDate.now());
        repository.save(existing);

        return repository.save(new RecurringCharge(type, amount, dueDay, description, fund, party, observations));
    }

    public void delete(Long id) {
        RecurringCharge existing = findById(id);
        existing.setDeactivatedAt(LocalDate.now());
        repository.save(existing);
    }

    private void validateNonNegativeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRecurringChargeAmountException();
        }
    }

    private Party findPartyOrThrow(Long partyId) {
        return partyRepository.findById(partyId).orElseThrow(() -> new NotFoundException("Parte não encontrada."));
    }

    private Group findGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new NotFoundException("Grupo não encontrado."));
    }

    private Fund findFundOrThrow(Long fundId) {
        return fundRepository.findById(fundId).orElseThrow(() -> new NotFoundException("Fundo não encontrado."));
    }
}
