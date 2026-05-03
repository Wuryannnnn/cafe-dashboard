package com.sell.repository;

import com.sell.dataobject.SettleAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettleAccountRepository extends JpaRepository<SettleAccount, Integer> {
    List<SettleAccount> findAllByOrderByAccountIdAsc();
}
