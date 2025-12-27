package com.votzz.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Libera CORS especificamente para o seu Front (Vite)
        registry.addEndpoint("/ws-votzz")
                .setAllowedOriginPatterns("http://localhost:5173", "*") 
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // Onde o front ESCUTA
        registry.setApplicationDestinationPrefixes("/app"); // Onde o front ENVIA
    }
}