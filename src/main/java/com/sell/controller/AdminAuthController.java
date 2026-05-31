package com.sell.controller;

import com.sell.dataobject.Staff;
import com.sell.repository.StaffRepository;
import com.sell.security.AdminAuthInterceptor;
import com.sell.security.AdminTokenService;
import com.sell.service.StaffService;
import com.sell.utils.CookieUtil;
import com.sell.utils.ResultVOUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台登录 (PRD 10 员工账号体系).
 * 登录成功后下发 HttpOnly 的 admin_token cookie, 由 AdminAuthInterceptor 校验.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private static final int COOKIE_MAX_AGE = 12 * 60 * 60; // 12h, 与令牌有效期一致

    @Autowired private StaffService staffService;
    @Autowired private StaffRepository staffRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AdminTokenService tokenService;

    @PostMapping("/login")
    public Object login(@RequestParam String username,
                        @RequestParam String password,
                        HttpServletResponse response) {
        Staff staff = staffService.findByUsername(username);
        if (staff == null || !verifyAndUpgrade(staff, password)) {
            return ResultVOUtil.error(1, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(staff.getEnabled())) {
            return ResultVOUtil.error(1, "账号已被禁用");
        }
        String token = tokenService.issue(staff.getStaffId(), staff.getUsername(), staff.getName(), staff.getRole());

        Cookie cookie = new Cookie(AdminAuthInterceptor.COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(cookie);

        return ResultVOUtil.success(userInfo(staff.getUsername(), staff.getName(), staff.getRole()));
    }

    @PostMapping("/logout")
    public Object logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie c = CookieUtil.get(request, AdminAuthInterceptor.COOKIE_NAME);
        if (c != null) {
            tokenService.revoke(c.getValue());
            Cookie del = new Cookie(AdminAuthInterceptor.COOKIE_NAME, "");
            del.setHttpOnly(true);
            del.setPath("/");
            del.setMaxAge(0);
            response.addCookie(del);
        }
        return ResultVOUtil.success();
    }

    @GetMapping("/me")
    public Object me(HttpServletRequest request) {
        Cookie c = CookieUtil.get(request, AdminAuthInterceptor.COOKIE_NAME);
        AdminTokenService.Session s = c == null ? null : tokenService.validate(c.getValue());
        if (s == null) {
            return ResultVOUtil.error(401, "未登录");
        }
        return ResultVOUtil.success(userInfo(s.username, s.name, s.role));
    }

    private Map<String, Object> userInfo(String username, String name, Integer role) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("name", name);
        data.put("role", role);
        return data;
    }

    /** BCrypt 校验; 兼容历史明文密码, 命中后自动升级为哈希存储. */
    private boolean verifyAndUpgrade(Staff staff, String rawPassword) {
        String stored = staff.getPassword();
        if (stored == null || stored.isEmpty() || rawPassword == null) {
            return false;
        }
        if (stored.startsWith("$2")) { // 已是 BCrypt 哈希
            return passwordEncoder.matches(rawPassword, stored);
        }
        // 历史明文密码
        if (stored.equals(rawPassword)) {
            staff.setPassword(passwordEncoder.encode(rawPassword));
            staffRepository.save(staff);
            return true;
        }
        return false;
    }
}
