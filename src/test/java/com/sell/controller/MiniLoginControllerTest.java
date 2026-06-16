package com.sell.controller;

import com.sell.service.WxMiniLoginService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@AutoConfigureMockMvc
public class MiniLoginControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private WxMiniLoginService loginService;

    @Test
    public void login_returnsTokenAndOpenid() throws Exception {
        when(loginService.login(eq("code-1")))
                .thenReturn(new WxMiniLoginService.LoginResult("openid-X", "tok-X"));
        mvc.perform(post("/mini/login").param("code", "code-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("tok-X"))
                .andExpect(jsonPath("$.data.openid").value("openid-X"));
    }
}
