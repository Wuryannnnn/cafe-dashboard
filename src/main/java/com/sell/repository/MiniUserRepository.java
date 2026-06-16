package com.sell.repository;

import com.sell.dataobject.MiniUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MiniUserRepository extends JpaRepository<MiniUser, String> {
    Optional<MiniUser> findByToken(String token);
}
