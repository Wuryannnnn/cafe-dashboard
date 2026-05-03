package com.sell.repository;

import com.sell.dataobject.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Integer> {
    Staff findByUsername(String username);
}
