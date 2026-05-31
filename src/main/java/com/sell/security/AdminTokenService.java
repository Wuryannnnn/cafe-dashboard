package com.sell.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台管理登录令牌存储 (内存版).
 * 本项目所有 profile 都禁用了 Redis (见 EmbeddedRedisConfig), 因此登录会话不依赖 Redis,
 * 与 PickupNumberLocalImpl 的内存实现保持一致. 单实例部署足够; 重启或多实例需重新登录.
 */
@Component
public class AdminTokenService {

    /** 令牌有效期 (毫秒): 12 小时. */
    private static final long TTL_MILLIS = 12L * 60 * 60 * 1000;

    /** 登录会话快照. */
    public static class Session {
        public final Integer staffId;
        public final String username;
        public final String name;
        public final Integer role;
        public final long expiresAt;

        Session(Integer staffId, String username, String name, Integer role, long expiresAt) {
            this.staffId = staffId;
            this.username = username;
            this.name = name;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, Session> store = new ConcurrentHashMap<>();

    /** 颁发令牌. */
    public String issue(Integer staffId, String username, String name, Integer role) {
        purgeExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, new Session(staffId, username, name, role, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    /** 校验令牌, 返回会话; 无效或过期返回 null. */
    public Session validate(String token) {
        if (token == null || token.isEmpty()) return null;
        Session s = store.get(token);
        if (s == null) return null;
        if (s.expiresAt < System.currentTimeMillis()) {
            store.remove(token);
            return null;
        }
        return s;
    }

    /** 注销令牌. */
    public void revoke(String token) {
        if (token != null) store.remove(token);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue().expiresAt < now);
    }
}
