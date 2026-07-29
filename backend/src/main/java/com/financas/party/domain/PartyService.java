package com.financas.party.domain;

import com.financas.account.domain.AccountRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PartyService {

    private final PartyRepository repository;
    private final AccountRepository accountRepository;

    public PartyService(PartyRepository repository, AccountRepository accountRepository) {
        this.repository = repository;
        this.accountRepository = accountRepository;
    }

    public Party create(String name, String pixKey) {
        validateNotDuplicate(name, null);
        return repository.save(new Party(name, pixKey));
    }

    public List<Party> findAll() {
        return repository.findAll();
    }

    public Party findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Parte não encontrada."));
    }

    public Party update(Long id, String name, String pixKey) {
        Party party = findById(id);
        validateNotDuplicate(name, id);
        party.setName(name);
        party.setPixKey(pixKey);
        return repository.save(party);
    }

    public void delete(Long id) {
        findById(id);
        if (accountRepository.existsByPartyId(id)) {
            throw new PartyHasAccountsException();
        }
        repository.deleteById(id);
    }

    private void validateNotDuplicate(String name, Long currentId) {
        repository.findByNormalizedName(name)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new DuplicatePartyException(name);
                });
    }
}
