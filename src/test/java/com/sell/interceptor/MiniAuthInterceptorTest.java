package com.sell.interceptor;

import com.sell.service.WxMiniLoginService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@AutoConfigureMockMvc
public class MiniAuthInterceptorTest {

    @Autowired private MockMvc mvc;
    @MockBean private WxMiniLoginService loginService;

    @Test
    public void protectedPath_noToken_unauthorized() throws Exception {
        mvc.perform(post("/mini/order/create").param("items", "[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void protectedPath_badToken_unauthorized() throws Exception {
        when(loginService.resolveOpenid("bad")).thenReturn(null);
        mvc.perform(post("/mini/order/create").header("Authorization", "bad").param("items", "[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginPath_excluded_notIntercepted() throws Exception {
        // /mini/login 被 excludePathPatterns 排除 → 不进拦截器, 直达 controller
        when(loginService.login("x"))
                .thenReturn(new WxMiniLoginService.LoginResult("o", "t"));
        mvc.perform(post("/mini/login").param("code", "x"))
                .andExpect(status().isOk());
    }
}
