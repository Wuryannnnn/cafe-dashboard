package com.sell.service.impl;

import com.sell.dataobject.Staff;
import com.sell.exception.SellException;
import com.sell.repository.StaffRepository;
import com.sell.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class StaffServiceImpl implements StaffService {

    @Autowired
    private StaffRepository repository;

    @Override
    public List<Staff> findAll() { return repository.findAll(); }

    @Override
    public Staff findOne(Integer staffId) {
        if (staffId == null) return null;
        return repository.findById(staffId).orElse(null);
    }

    @Override
    public Staff findByUsername(String username) {
        if (username == null || username.isEmpty()) return null;
        return repository.findByUsername(username);
    }

    @Override
    public Staff save(Staff staff) {
        if (staff.getUsername() == null || staff.getUsername().trim().isEmpty()) {
            throw new SellException(1, "用户名不能为空");
        }
        Staff exist = repository.findByUsername(staff.getUsername());
        if (exist != null && !exist.getStaffId().equals(staff.getStaffId())) {
            throw new SellException(1, "用户名已存在");
        }
        if (staff.getRole() == null) staff.setRole(2);
        if (staff.getEnabled() == null) staff.setEnabled(true);
        Date now = new Date();
        if (staff.getCreateTime() == null) staff.setCreateTime(now);
        staff.setUpdateTime(now);
        return repository.save(staff);
    }

    @Override
    public void delete(Integer staffId) {
        repository.deleteById(staffId);
    }

    @Override
    public void toggleEnabled(Integer staffId) {
        Staff s = findOne(staffId);
        if (s == null) throw new SellException(1, "员工不存在");
        s.setEnabled(!Boolean.TRUE.equals(s.getEnabled()));
        s.setUpdateTime(new Date());
        repository.save(s);
    }
}
