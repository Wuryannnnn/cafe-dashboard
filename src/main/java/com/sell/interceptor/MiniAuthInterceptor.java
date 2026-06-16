package com.sell.interceptor;

import com.sell.service.WxMiniLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 小程序顾客鉴权: 校验 Authorization token → 注入 miniOpenid; 无效则 401.
 * 独立于店主后台 AdminAuth (各拦各的前缀, 见 MiniWebConfig).
 */
@Component
public class MiniAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_OPENID = "miniOpenid";

    @Autowired
    private WxMiniLoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String token = req.getHeader("Authorization");
        String openid = loginService.resolveOpenid(token);
        if (openid == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":401,\"msg\":\"登录已过期, 请重新进入\"}");
            return false;
        }
        req.setAttribute(ATTR_OPENID, openid);
        return true;
    }
}
