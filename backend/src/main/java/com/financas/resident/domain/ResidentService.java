package com.financas.resident.domain;

import com.financas.shared.exceptions.NotFoundException;
import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResidentService {

    private final ResidentRepository repository;
    private final UnitRepository unitRepository;

    public ResidentService(ResidentRepository repository, UnitRepository unitRepository) {
        this.repository = repository;
        this.unitRepository = unitRepository;
    }

    public Resident create(String name, Long unitId, String email, String phone) {
        Unit unit = findUnitOrThrow(unitId);
        return repository.save(new Resident(name, unit, email, phone));
    }

    public List<Resident> findAll() {
        return repository.findAll();
    }

    public Resident findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Condômino não encontrado."));
    }

    public Resident update(Long id, String name, Long unitId, String email, String phone) {
        Resident resident = findById(id);
        Unit unit = findUnitOrThrow(unitId);
        resident.setName(name);
        resident.setUnit(unit);
        resident.setEmail(email);
        resident.setPhone(phone);
        return repository.save(resident);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Condômino não encontrado.");
        }
        repository.deleteById(id);
    }

    private Unit findUnitOrThrow(Long unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new NotFoundException(
                        "Unidade não encontrada. Cadastre uma unidade antes de associar um condômino a ela."));
    }
}
