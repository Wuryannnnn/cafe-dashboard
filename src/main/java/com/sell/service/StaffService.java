package com.sell.service;

import com.sell.dataobject.Staff;

import java.util.List;

public interface StaffService {
    List<Staff> findAll();
    Staff findOne(Integer staffId);
    Staff findByUsername(String username);
    Staff save(Staff staff);
    void delete(Integer staffId);
    void toggleEnabled(Integer staffId);
}
