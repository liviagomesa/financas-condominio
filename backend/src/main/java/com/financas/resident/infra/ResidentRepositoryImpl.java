package com.financas.resident.infra;

import com.financas.resident.domain.Resident;
import com.financas.resident.domain.ResidentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ResidentRepositoryImpl implements ResidentRepository {

    private final ResidentJpaRepository jpaRepository;

    public ResidentRepositoryImpl(ResidentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Resident save(Resident resident) {
        return jpaRepository.save(resident);
    }

    @Override
    public Optional<Resident> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Resident> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByUnitId(Long unitId) {
        return jpaRepository.existsByUnitId(unitId);
    }
}
