package com.financas.group.infra;

import com.financas.group.domain.Group;
import com.financas.group.domain.GroupRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupJpaRepository jpaRepository;

    public GroupRepositoryImpl(GroupJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Group save(Group group) {
        return jpaRepository.save(group);
    }

    @Override
    public Optional<Group> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Group> findAll() {
        return jpaRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Optional<Group> findByNormalizedName(String name) {
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
