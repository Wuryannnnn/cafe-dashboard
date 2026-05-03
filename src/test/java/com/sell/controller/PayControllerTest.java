package com.sell.controller;

import com.lly835.bestpay.model.PayResponse;
import com.sell.dto.OrderDTO;
import com.sell.enums.PayTypeEnum;
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
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void create_payTypeAlipay_returnsAlipayPayloadWithSandboxUrlPatched() throws Exception {
        OrderDTO order = new OrderDTO();
        order.setOrderId("O1");
        order.setOrderAmount(new BigDecimal("9.99"));
        when(orderService.findOne("O1")).thenReturn(order);

        PayResponse resp = new PayResponse();
        resp.setBody("https://openapi.alipaydev.com//gateway?biz_content=xxx");
        resp.setPayUri(new URI("https://openapi.alipaydev.com/gateway?x=1"));
        when(payService.createAlipay(any(OrderDTO.class))).thenReturn(resp);

        mvc.perform(get("/pay/create")
                        .param("orderId", "O1")
                        .param("returnUrl", "/order/status")
                        .param("payType", String.valueOf(PayTypeEnum.ALIPAY.getCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payType").value("alipay"))
                .andExpect(jsonPath("$.returnUrl").value("/order/status"))
                .andExpect(jsonPath("$.body").value(
                        "https://openapi-sandbox.dl.alipaydev.com/gateway?biz_content=xxx"))
                .andExpect(jsonPath("$.payUri").value(
                        "https://openapi-sandbox.dl.alipaydev.com/gateway?x=1"));
    }

    @Test
    public void create_noPayType_defaultsToWechat() throws Exception {
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

    @Test
    public void alipayNotify_returnsLiteralSuccess() throws Exception {
        when(payService.alipayNotify(any(String.class))).thenReturn(new PayResponse());

        mvc.perform(post("/pay/alipay/notify")
                        .contentType("application/x-www-form-urlencoded")
                        .content("trade_status=TRADE_SUCCESS&out_trade_no=O1"))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    @Test
    public void patchSandboxUrl_doubleSlashFixedAndDomainReplaced() {
        assertThat(PayController.patchSandboxUrl(
                "https://openapi.alipaydev.com//gateway?a=1"))
                .isEqualTo("https://openapi-sandbox.dl.alipaydev.com/gateway?a=1");
    }

    @Test
    public void patchSandboxUrl_singleSlashDomainReplaced() {
        assertThat(PayController.patchSandboxUrl(
                "https://openapi.alipaydev.com/gateway?a=1"))
                .isEqualTo("https://openapi-sandbox.dl.alipaydev.com/gateway?a=1");
    }

    @Test
    public void patchSandboxUrl_bareDomainReplaced() {
        assertThat(PayController.patchSandboxUrl("openapi.alipaydev.com"))
                .isEqualTo("openapi-sandbox.dl.alipaydev.com");
    }

    @Test
    public void patchSandboxUrl_unrelatedUrlUntouched() {
        assertThat(PayController.patchSandboxUrl("https://example.com/foo"))
                .isEqualTo("https://example.com/foo");
    }

    @Test
    public void patchSandboxUrl_nullSafe() {
        assertThat(PayController.patchSandboxUrl(null)).isNull();
    }
}
