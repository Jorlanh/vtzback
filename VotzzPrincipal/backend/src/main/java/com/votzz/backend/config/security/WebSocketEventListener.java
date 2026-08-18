package com.votzz.backend.config.security;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebSocketEventListener {

    private final AtomicInteger onlineUsers = new AtomicInteger(0);

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        onlineUsers.incrementAndGet();
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        onlineUsers.decrementAndGet();
    }

    public int getOnlineCount() {
        return Math.max(0, onlineUsers.get());
    }
}