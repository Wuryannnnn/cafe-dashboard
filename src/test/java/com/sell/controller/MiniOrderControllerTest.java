package com.sell.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sell.dataobject.OrderMaster;
import com.sell.dataobject.ProductInfo;
import com.sell.repository.OrderMasterRepository;
import com.sell.repository.ProductInfoRepository;
import com.sell.service.WxMiniLoginService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "admin.auth.enabled=false")
@AutoConfigureMockMvc
@Transactional
public class MiniOrderControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ProductInfoRepository productRepo;
    @Autowired private OrderMasterRepository orderMasterRepo;
    @MockBean private WxMiniLoginService loginService;

    @Before
    public void seed() {
        ProductInfo p = new ProductInfo();
        p.setProductId("MINI_P1");
        p.setProductName("拿铁");
        p.setProductPrice(new BigDecimal("18.00"));
        p.setProductStock(100);
        p.setProductStatus(0);
        p.setCategoryType(1);
        productRepo.save(p);
        when(loginService.resolveOpenid("tok-1")).thenReturn("openid-buyer-1");
    }

    @Test
    public void create_usesTokenOpenid_andForgedOpenidIgnored() throws Exception {
        MvcResult res = mvc.perform(post("/mini/order/create")
                        .header("Authorization", "tok-1")
                        .param("tableId", "5")
                        .param("openid", "FORGED-openid") // 应被忽略
                        .param("items", "[{\"productId\":\"MINI_P1\",\"productQuantity\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").exists())
                .andReturn();

        JsonObject root = JsonParser.parseString(res.getResponse().getContentAsString()).getAsJsonObject();
        String orderId = root.getAsJsonObject("data").get("orderId").getAsString();
        OrderMaster om = orderMasterRepo.findById(orderId).orElse(null);
        assertNotNull(om);
        assertEquals("买家 openid 必须取自 token, 而非前端伪造值", "openid-buyer-1", om.getBuyerOpenid());
        assertEquals("默认就餐方式应为堂食(0)", Integer.valueOf(0), om.getDiningType());
    }

    @Test
    public void create_takeaway_setsDiningType1_andDropsTable() throws Exception {
        // 外带: diningType=1; 即便误传 tableId 也应被丢弃(外带无桌台)
        MvcResult res = mvc.perform(post("/mini/order/create")
                        .header("Authorization", "tok-1")
                        .param("tableId", "5")
                        .param("diningType", "1")
                        .param("items", "[{\"productId\":\"MINI_P1\",\"productQuantity\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").exists())
                .andReturn();

        JsonObject root = JsonParser.parseString(res.getResponse().getContentAsString()).getAsJsonObject();
        String orderId = root.getAsJsonObject("data").get("orderId").getAsString();
        OrderMaster om = orderMasterRepo.findById(orderId).orElse(null);
        assertNotNull(om);
        assertEquals("外带就餐方式应为1", Integer.valueOf(1), om.getDiningType());
        assertNull("外带不应绑定桌台", om.getTableId());
    }
}
