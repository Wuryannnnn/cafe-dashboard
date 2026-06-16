package com.sell.controller;

import com.lly835.bestpay.model.PayResponse;
import com.sell.dto.OrderDTO;
import com.sell.service.OrderService;
import com.sell.service.PayService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 公众号 best-pay 微信支付端点 (支付宝已下线). */
@RunWith(MockitoJUnitRunner.class)
public class PayControllerTest {

    @Mock private OrderService orderService;
    @Mock private PayService payService;
    @InjectMocks private PayController controller;

    private MockMvc mvc;

    @Before
    public void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void create_returnsWechatPayload() throws Exception {
        OrderDTO order = new OrderDTO();
        order.setOrderId("O2");
        order.setOrderAmount(new BigDecimal("1.00"));
        when(orderService.findOne("O2")).thenReturn(order);
        when(payService.create(any(OrderDTO.class))).thenReturn(new PayResponse());

        mvc.perform(get("/pay/create")
                        .param("orderId", "O2")
                        .param("returnUrl", "/x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payType").value("wechat"));
    }

    @Test
    public void wechatNotify_returnsSuccessXml() throws Exception {
        when(payService.notify(any(String.class))).thenReturn(new PayResponse());

        mvc.perform(post("/pay/notify")
                        .contentType("application/xml")
                        .content("<xml><out_trade_no>O1</out_trade_no></xml>"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<return_code><![CDATA[SUCCESS]]></return_code>")));
    }
}
