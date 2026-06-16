package com.sell.service;

import com.sell.dataobject.MiniUser;
import com.sell.exception.SellException;
import com.sell.repository.MiniUserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class WxMiniLoginServiceTest {

    @Autowired private WxMiniLoginService loginService;
    @Autowired private MiniUserRepository repo;
    @MockBean private Jscode2SessionClient jscode2SessionClient;

    @Test
    public void login_upsertsUserAndIssuesToken() {
        when(jscode2SessionClient.exchange("good-code"))
                .thenReturn(new Jscode2SessionClient.Session("openid-A", "sk-A"));

        WxMiniLoginService.LoginResult r = loginService.login("good-code");

        assertEquals("openid-A", r.openid);
        assertNotNull(r.token);
        MiniUser saved = repo.findById("openid-A").orElse(null);
        assertNotNull(saved);
        assertEquals("sk-A", saved.getSessionKey()); // sessionKey 留后端
        assertEquals(r.token, saved.getToken());
    }

    @Test(expected = SellException.class)
    public void login_wechatError_throwsReadable() {
        when(jscode2SessionClient.exchange("bad-code")).thenReturn(null); // errcode!=0 → null
        loginService.login("bad-code");
    }

    @Test(expected = SellException.class)
    public void login_blankCode_throws() {
        loginService.login("  ");
    }

    @Test
    public void resolveOpenid_validToken_returnsOpenid() {
        when(jscode2SessionClient.exchange("c"))
                .thenReturn(new Jscode2SessionClient.Session("openid-B", "sk"));
        String token = loginService.login("c").token;
        assertEquals("openid-B", loginService.resolveOpenid(token));
    }

    @Test
    public void resolveOpenid_unknownToken_returnsNull() {
        assertNull(loginService.resolveOpenid("no-such-token"));
    }
}
