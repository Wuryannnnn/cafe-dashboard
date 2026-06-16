package com.sell.service;

import com.sell.dataobject.MiniUser;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.repository.MiniUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

/** 小程序登录: jscode2session 换身份 + 签发/校验不透明 token. */
@Service
@Slf4j
public class WxMiniLoginService {

    /** token 有效期 (2 天, 短于 session_key). */
    private static final long TOKEN_TTL_MS = 2L * 24 * 3600 * 1000;

    @Autowired
    private MiniUserRepository repo;

    @Autowired
    private Jscode2SessionClient jscode2SessionClient;

    public static class LoginResult {
        public final String openid;
        public final String token;
        public LoginResult(String openid, String token) {
            this.openid = openid;
            this.token = token;
        }
    }

    @Transactional
    public LoginResult login(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new SellException(ResultEnum.PARAM_ERROR.getCode(), "code 不能为空");
        }
        Jscode2SessionClient.Session s = jscode2SessionClient.exchange(code);
        if (s == null || s.openid == null) {
            throw new SellException(ResultEnum.PARAM_ERROR.getCode(), "微信登录失败, 请重试");
        }
        Date now = new Date();
        MiniUser u = repo.findById(s.openid).orElseGet(() -> {
            MiniUser n = new MiniUser();
            n.setOpenid(s.openid);
            n.setCreateTime(now);
            return n;
        });
        u.setSessionKey(s.sessionKey);
        u.setToken(UUID.randomUUID().toString().replace("-", ""));
        u.setTokenExpireAt(new Date(now.getTime() + TOKEN_TTL_MS));
        u.setUpdateTime(now);
        repo.save(u);
        return new LoginResult(u.getOpenid(), u.getToken());
    }

    /** 校验 token, 返回 openid; 无效/过期返回 null. */
    public String resolveOpenid(String token) {
        if (token == null || token.isEmpty()) return null;
        MiniUser u = repo.findByToken(token).orElse(null);
        if (u == null || u.getTokenExpireAt() == null || u.getTokenExpireAt().before(new Date())) {
            return null;
        }
        return u.getOpenid();
    }
}
