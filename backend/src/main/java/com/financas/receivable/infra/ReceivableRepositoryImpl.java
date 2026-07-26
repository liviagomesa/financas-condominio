package com.financas.receivable.infra;

import com.financas.receivable.domain.Receivable;
import com.financas.receivable.domain.ReceivableRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ReceivableRepositoryImpl implements ReceivableRepository {

    private final ReceivableJpaRepository jpaRepository;

    public ReceivableRepositoryImpl(ReceivableJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Receivable save(Receivable receivable) {
        return jpaRepository.save(receivable);
    }

    @Override
    public Optional<Receivable> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Receivable> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Receivable> findByUnitId(Long unitId) {
        return jpaRepository.findByUnitId(unitId);
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
