package com.financas.fund.infra;

import com.financas.fund.domain.Fund;
import com.financas.fund.domain.FundRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class FundRepositoryImpl implements FundRepository {

    private final FundJpaRepository jpaRepository;

    public FundRepositoryImpl(FundJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Fund save(Fund fund) {
        return jpaRepository.save(fund);
    }

    @Override
    public Optional<Fund> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Fund> findAll() {
        return jpaRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Optional<Fund> findByNormalizedName(String name) {
        return jpaRepository.findByNormalizedName(name);
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
