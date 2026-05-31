package com.sell.config;

import com.sell.dataobject.Staff;
import com.sell.repository.StaffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

/** 首次启动若无任何员工账号, 创建默认老板账号, 以便后台能登录 (启用鉴权前的引导). */
@Component
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (staffRepository.count() > 0) {
            return;
        }
        Staff s = new Staff();
        s.setUsername("admin");
        s.setPassword(passwordEncoder.encode("admin888"));
        s.setName("管理员");
        s.setRole(0); // 老板
        s.setEnabled(true);
        s.setCreateTime(new Date());
        s.setUpdateTime(new Date());
        staffRepository.save(s);
        log.warn("============================================================");
        log.warn(" 已创建默认后台账号 admin / admin888 (角色: 老板)");
        log.warn(" 请登录后立即在【员工管理】修改密码!");
        log.warn("============================================================");
    }
}
