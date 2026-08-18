package com.votzz.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Libera CORS para qualquer origem (desenvolvimento)
        // Em produção, troque "*" pelo seu domínio real (ex: "https://app.votzz.com")
        registry.addEndpoint("/ws-votzz")
                .setAllowedOriginPatterns("*") 
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefixo para mensagens enviadas DO servidor PARA o cliente
        registry.enableSimpleBroker("/topic", "/queue"); 
        
        // Prefixo para mensagens enviadas DO cliente PARA o servidor
        registry.setApplicationDestinationPrefixes("/app"); 
    }
}