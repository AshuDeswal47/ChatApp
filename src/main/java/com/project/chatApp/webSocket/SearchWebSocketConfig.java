package com.project.chatApp.webSocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class SearchWebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(SearchWebSocketHandler(), "/ws/search").addInterceptors(new CustomHandshakeInterceptor()).setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler SearchWebSocketHandler() {
        return new SearchWebSocketHandler();
    }

}
