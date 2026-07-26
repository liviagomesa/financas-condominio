package com.financas.unit.infra;

import com.financas.unit.domain.Unit;
import com.financas.unit.domain.UnitRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UnitRepositoryImpl implements UnitRepository {

    private final UnitJpaRepository jpaRepository;

    public UnitRepositoryImpl(UnitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Unit save(Unit unit) {
        return jpaRepository.save(unit);
    }

    @Override
    public Optional<Unit> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Unit> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<Unit> findByNormalizedIdentifier(String identifier) {
        return jpaRepository.findByNormalizedIdentifier(identifier);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
