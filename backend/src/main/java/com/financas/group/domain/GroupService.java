package com.financas.group.domain;

import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

    private final GroupRepository repository;
    private final PartyRepository partyRepository;

    public GroupService(GroupRepository repository, PartyRepository partyRepository) {
        this.repository = repository;
        this.partyRepository = partyRepository;
    }

    public Group create(String name, List<Long> partyIds) {
        validateNotDuplicate(name, null);
        Set<Party> members = resolveMembers(partyIds);
        return repository.save(new Group(name, members));
    }

    public List<Group> findAll() {
        return repository.findAll();
    }

    public Group findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Grupo não encontrado."));
    }

    public Group update(Long id, String name, List<Long> partyIds) {
        Group group = findById(id);
        validateNotDuplicate(name, id);
        group.setName(name);
        group.setMembers(resolveMembers(partyIds));
        return repository.save(group);
    }

    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }

    private Set<Party> resolveMembers(List<Long> partyIds) {
        if (partyIds == null || partyIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Party> members = new LinkedHashSet<>();
        for (Long partyId : partyIds) {
            members.add(partyRepository
                    .findById(partyId)
                    .orElseThrow(() -> new NotFoundException("Parte não encontrada.")));
        }
        return members;
    }

    private void validateNotDuplicate(String name, Long currentId) {
        repository.findByNormalizedName(name)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new DuplicateGroupException(name);
                });
    }
}
