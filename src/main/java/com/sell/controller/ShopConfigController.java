package com.sell.controller;

import com.sell.VO.ResultVO;
import com.sell.dataobject.ShopConfig;
import com.sell.repository.ShopConfigRepository;
import com.sell.utils.ResultVOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 店铺配置API(给H5前端用)
 */
@RestController
@RequestMapping("/buyer/shop")
public class ShopConfigController {

    @Autowired
    private ShopConfigRepository shopConfigRepository;

    @GetMapping("/config")
    public ResultVO<Map<String, String>> getConfig() {
        List<ShopConfig> configs = shopConfigRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (ShopConfig c : configs) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return ResultVOUtil.success(map);
    }
}
