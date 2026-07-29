package com.financas.account.infra;

import com.financas.account.domain.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<Account, Long> {

    List<Account> findByPartyId(Long partyId);

    List<Account> findByFundId(Long fundId);

    boolean existsByPartyId(Long partyId);

    boolean existsByFundId(Long fundId);
}
