package com.stark.server;

// This file is now editable
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(new String[]{"/ws"});
        registry.addEndpoint(new String[]{"/ws"}).setAllowedOriginPatterns(new String[]{"*"}).withSockJS();
    }

    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(new String[]{"/topic"});
        registry.setApplicationDestinationPrefixes(new String[]{"/app"});
    }
}
