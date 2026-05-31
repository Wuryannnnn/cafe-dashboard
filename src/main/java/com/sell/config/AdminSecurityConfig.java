package com.sell.config;

import com.sell.security.AdminAuthInterceptor;
import com.sell.security.AdminTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 后台鉴权配置.
 * - 提供 BCrypt 密码编码器.
 * - 当 admin.auth.enabled=true 时, 对 /api/admin/** 与 /seller/** 启用登录校验.
 *   默认 false: 机制就位但不拦截, 待前端接好登录后再开启, 避免中途打断现有页面.
 */
@Configuration
public class AdminSecurityConfig implements WebMvcConfigurer {

    @Value("${admin.auth.enabled:false}")
    private boolean authEnabled;

    private final AdminTokenService tokenService;

    public AdminSecurityConfig(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!authEnabled) {
            return;
        }
        registry.addInterceptor(new AdminAuthInterceptor(tokenService))
                .addPathPatterns("/api/admin/**", "/seller/**")
                .excludePathPatterns(
                        "/api/admin/login",
                        "/api/admin/logout",
                        "/api/admin/me",
                        "/seller/login",   // 旧微信登录回调
                        "/seller/logout"
                );
    }
}
