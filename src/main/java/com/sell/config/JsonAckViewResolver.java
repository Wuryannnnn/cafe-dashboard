package com.sell.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 把老 FreeMarker 视图名 `common/success` / `common/error` 映射到 JSON 响应,
 * 避免要修改 30+ 控制器. 优先级高于 FreeMarker 默认 ViewResolver.
 */
@Configuration
public class JsonAckViewResolver implements ViewResolver, Ordered {

    private static final MappingJackson2JsonView SUCCESS_VIEW = makeView(0);
    private static final MappingJackson2JsonView ERROR_VIEW = makeView(1);

    private static MappingJackson2JsonView makeView(int code) {
        MappingJackson2JsonView v = new MappingJackson2JsonView();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("code", code);
        v.setAttributesMap(attrs);
        v.setExtractValueFromSingleKeyModel(false);
        return v;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) {
        if ("common/success".equals(viewName)) return SUCCESS_VIEW;
        if ("common/error".equals(viewName)) return ERROR_VIEW;
        return null;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
