package com.project.chatApp.webSocket;

import io.jsonwebtoken.security.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class MessageWebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;

    // Constructor injection to automatically inject the WebSocket handler bean
    public MessageWebSocketConfig(MessageWebSocketHandler messageWebSocketHandler) {
        this.messageWebSocketHandler = messageWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(new CustomHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    // WebSocket handler bean definition
    @Bean
    public MessageWebSocketHandler messageWebSocketHandler() {
        return new MessageWebSocketHandler();
    }

}
