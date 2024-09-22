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

    MessageWebSocketHandler messageWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(createNewMessageWebSocketHandler(), "/ws/message").addInterceptors(new CustomHandshakeInterceptor()).setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler createNewMessageWebSocketHandler() {
        messageWebSocketHandler = new MessageWebSocketHandler();
        return messageWebSocketHandler;
    }

    @Bean
    public MessageWebSocketHandler getWebSocketHandler() {
        return messageWebSocketHandler;
    }

}
