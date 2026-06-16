package com.sell.controller;

import com.sell.VO.ResultVO;
import com.sell.service.WxMiniLoginService;
import com.sell.utils.ResultVOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/mini")
public class MiniLoginController {

    @Autowired
    private WxMiniLoginService loginService;

    /** 小程序登录: 收 wx.login 的 code → 换 openid + 下发自建 token. */
    @PostMapping("/login")
    public ResultVO<Map<String, String>> login(@RequestParam("code") String code) {
        WxMiniLoginService.LoginResult r = loginService.login(code);
        Map<String, String> m = new HashMap<>();
        m.put("token", r.token);
        m.put("openid", r.openid);
        return ResultVOUtil.success(m);
    }
}
