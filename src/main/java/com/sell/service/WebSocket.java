package com.sell.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 2017-07-30 23:19
 */
@Component
@ServerEndpoint("/webSocket")
@Slf4j
public class WebSocket {

    private Session session;

    private static CopyOnWriteArraySet<WebSocket> webSocketSet = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);
        log.info("【websocket消息】有新的连接, 总数:{}", webSocketSet.size());
    }

    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
        log.info("【websocket消息】连接断开, 总数:{}", webSocketSet.size());
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【websocket消息】收到客户端发来的消息:{}", message);
    }

    public void sendMessage(String message) {
        for (WebSocket webSocket: webSocketSet) {
            Session s = webSocket.session;
            if (s == null || !s.isOpen()) {
                webSocketSet.remove(webSocket); // 清理已关闭/无效会话
                continue;
            }
            try {
                // getBasicRemote() 是同步的, 同一 session 并发调用会抛 IllegalStateException;
                // 按 session 加锁串行化发送
                synchronized (s) {
                    s.getBasicRemote().sendText(message);
                }
            } catch (Exception e) {
                log.warn("【websocket消息】发送失败, 移除会话: {}", e.getMessage());
                webSocketSet.remove(webSocket);
            }
        }
    }

}
