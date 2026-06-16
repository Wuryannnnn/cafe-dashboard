package com.sell.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sell.VO.ResultVO;
import com.sell.dataobject.OrderDetail;
import com.sell.dto.OrderDTO;
import com.sell.enums.ResultEnum;
import com.sell.exception.SellException;
import com.sell.interceptor.MiniAuthInterceptor;
import com.sell.service.OrderService;
import com.sell.utils.ResultVOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 小程序下单: openid 由鉴权拦截器从 token 注入, 忽略前端传入的 openid (防伪造). */
@RestController
@RequestMapping("/mini/order")
public class MiniOrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public ResultVO<Map<String, String>> create(@RequestParam(value = "tableId", required = false) Integer tableId,
                                                 @RequestParam("items") String items,
                                                 HttpServletRequest request) {
        String openid = (String) request.getAttribute(MiniAuthInterceptor.ATTR_OPENID);

        List<OrderDetail> details;
        try {
            details = new Gson().fromJson(items, new TypeToken<List<OrderDetail>>(){}.getType());
        } catch (Exception e) {
            throw new SellException(ResultEnum.PARAM_ERROR);
        }
        if (details == null || details.isEmpty()) {
            throw new SellException(ResultEnum.CART_EMPTY);
        }

        OrderDTO dto = new OrderDTO();
        dto.setBuyerOpenid(openid);   // 服务端注入, 忽略前端 openid
        dto.setBuyerName("微信顾客");
        dto.setBuyerPhone("");
        dto.setBuyerAddress("");
        dto.setDiningType(0);
        dto.setTableId(tableId);
        dto.setOrderDetailList(details);
        OrderDTO created = orderService.create(dto);

        Map<String, String> m = new HashMap<>();
        m.put("orderId", created.getOrderId());
        m.put("pickupNumber", created.getPickupNumber());
        return ResultVOUtil.success(m);
    }
}
