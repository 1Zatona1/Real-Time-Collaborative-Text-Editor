package com.stark.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TextEditorHandler textEditorHandler;

    public WebSocketConfig(TextEditorHandler textEditorHandler) {
        this.textEditorHandler = textEditorHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(textEditorHandler, "/ws")
                .setAllowedOrigins("*")
                .withSockJS(); // Add SockJS for fallback support
    }
}