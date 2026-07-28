package com.financas.supplier.infra;

import com.financas.supplier.domain.Supplier;
import com.financas.supplier.domain.SupplierRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SupplierRepositoryImpl implements SupplierRepository {

    private final SupplierJpaRepository jpaRepository;

    public SupplierRepositoryImpl(SupplierJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Supplier save(Supplier supplier) {
        return jpaRepository.save(supplier);
    }

    @Override
    public Optional<Supplier> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Supplier> findAll() {
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
