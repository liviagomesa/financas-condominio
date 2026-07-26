package com.financas.unit.domain;

import com.financas.receivable.domain.ReceivableRepository;
import com.financas.resident.domain.ResidentRepository;
import com.financas.shared.exceptions.NotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class UnitService {

    private final UnitRepository repository;
    private final ResidentRepository residentRepository;
    private final ReceivableRepository receivableRepository;

    public UnitService(
            UnitRepository repository,
            ResidentRepository residentRepository,
            ReceivableRepository receivableRepository) {
        this.repository = repository;
        this.residentRepository = residentRepository;
        this.receivableRepository = receivableRepository;
    }

    public Unit create(String identifier) {
        validateNotDuplicate(identifier, null);
        return repository.save(new Unit(identifier));
    }

    public List<Unit> findAll() {
        return repository.findAll();
    }

    public Unit findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unidade não encontrada."));
    }

    public Unit update(Long id, String identifier) {
        Unit unit = findById(id);
        validateNotDuplicate(identifier, id);
        unit.setIdentifier(identifier);
        return repository.save(unit);
    }

    public void delete(Long id) {
        findById(id);
        if (residentRepository.existsByUnitId(id)) {
            throw new UnitHasResidentsException();
        }
        if (receivableRepository.existsByUnitId(id)) {
            throw new UnitHasReceivablesException();
        }
        repository.deleteById(id);
    }

    private void validateNotDuplicate(String identifier, Long currentId) {
        repository.findByNormalizedIdentifier(identifier)
                .filter(existing -> !Objects.equals(existing.getId(), currentId))
                .ifPresent(existing -> {
                    throw new DuplicateUnitException(identifier);
                });
    }
}
