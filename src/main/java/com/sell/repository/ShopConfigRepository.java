package com.sell.repository;

import com.sell.dataobject.ShopConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopConfigRepository extends JpaRepository<ShopConfig, String> {
}
