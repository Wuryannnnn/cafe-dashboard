package com.sell.service;

import com.sell.repository.PendingShippingRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@Transactional
public class MiniShippingServiceTest {

    @Autowired private MiniShippingService shippingService;
    @Autowired private PendingShippingRepository repo;
    @MockBean private WxMiniAccessTokenService accessTokenService;

    @Test
    public void report_noTransactionId_skips() {
        shippingService.report("O1", "openid", null);
        assertEquals(0, repo.count()); // 无交易号(现金/历史单)不上报、不入队
    }

    @Test
    public void report_unconfigured_skipsWithoutEnqueue() {
        when(accessTokenService.getAccessToken()).thenReturn(null);
        shippingService.report("O1", "openid", "TX1");
        assertEquals(0, repo.count()); // 未配置凭据不入队(避免堆垃圾)
    }
}
