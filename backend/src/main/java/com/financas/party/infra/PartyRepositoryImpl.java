package com.financas.party.infra;

import com.financas.party.domain.Party;
import com.financas.party.domain.PartyRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PartyRepositoryImpl implements PartyRepository {

    private final PartyJpaRepository jpaRepository;

    public PartyRepositoryImpl(PartyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Party save(Party party) {
        return jpaRepository.save(party);
    }

    @Override
    public Optional<Party> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Party> findAll() {
        return jpaRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Optional<Party> findByNormalizedName(String name) {
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
