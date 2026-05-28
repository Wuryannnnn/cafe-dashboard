package com.sell.security;

import com.sell.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台 API 鉴权拦截器: 校验 admin_token cookie 对应的登录会话, 未登录返回 401 JSON.
 * 是否启用由 admin.auth.enabled 开关控制 (见 AdminSecurityConfig).
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String COOKIE_NAME = "admin_token";
    public static final String SESSION_ATTR = "ADMIN_SESSION";

    private final AdminTokenService tokenService;

    public AdminAuthInterceptor(AdminTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Cookie cookie = CookieUtil.get(request, COOKIE_NAME);
        AdminTokenService.Session session = cookie == null ? null : tokenService.validate(cookie.getValue());
        if (session == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录或登录已过期");
            return false;
        }
        request.setAttribute(SESSION_ATTR, session);

        // 角色权限校验 (去除 context-path 后按模块前缀匹配)
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        if (!AdminPermission.allowed(session.role, request.getMethod(), path)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权访问该功能");
            return false;
        }
        return true;
    }

    private void writeJson(HttpServletResponse response, int httpStatus, int code, String msg) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
    }
}
