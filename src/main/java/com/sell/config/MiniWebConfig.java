package com.sell.config;

import com.sell.interceptor.MiniAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 注册小程序顾客鉴权拦截器 (独立于店主后台 admin.auth, 各拦各的前缀). */
@Configuration
public class MiniWebConfig implements WebMvcConfigurer {

    @Autowired
    private MiniAuthInterceptor miniAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(miniAuthInterceptor)
                .addPathPatterns("/mini/order/**", "/pay/mini/create")
                .excludePathPatterns("/mini/login", "/pay/mini/notify");
    }
}
