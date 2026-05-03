package com.sell.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * 2017-07-30 23:17
 *
 * 默认开启; mvn test 时 MockServletContext 拿不到 jakarta.websocket.server.ServerContainer,
 * 整个 SpringBoot context 起不来. 通过 application-test 资源里把 websocket.enabled=false 关掉.
 */
@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
