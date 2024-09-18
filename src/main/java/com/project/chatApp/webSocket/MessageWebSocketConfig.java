package com.project.chatApp.webSocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class MessageWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(MessageWebSocketHandler(), "/ws/message").addInterceptors(new CustomHandshakeInterceptor()).setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler MessageWebSocketHandler() {
        return new MessageWebSocketHandler();
    }

}
