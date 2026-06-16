package com.sell.controller;

import com.sell.service.WxMiniAccessTokenService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@AutoConfigureMockMvc
public class MiniQrCodeControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private WxMiniAccessTokenService accessTokenService;

    @Test
    public void mini_unconfigured_returns503() throws Exception {
        when(accessTokenService.getAccessToken()).thenReturn(null);
        mvc.perform(get("/seller/qrcode/mini").param("tableId", "5"))
                .andExpect(status().isServiceUnavailable());
    }
}
