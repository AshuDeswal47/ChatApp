package com.project.chatApp.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chatApp.dataTransferObject.PublicUserDTO;
import com.project.chatApp.service.ConversationService;
import com.project.chatApp.service.MessageService;
import com.project.chatApp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SearchWebSocketHandler extends TextWebSocketHandler {

    // active connections with key : username
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserService userService;

    @Autowired
    MessageService messageService;

    @Autowired
    ConversationService conversationService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String username = getUsername(session);
            if (username != null) {
                // store active connections in hashMap
                sessions.put(username, session);
                log.info("SearchWebSocket user connected : {} ", username);
            } else {
                log.info("No authentication found");
            }
        } catch (Exception e) {
            log.error("AfterConnectionEstablished : {} ", String.valueOf(e));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            String username = getUsername(session);
            if (username != null) {
                // remove the disconnected webSocket from hashMap
                sessions.remove(username);
                log.info("SearchWebSocket user disconnected : {} ", username);
            } else {
                log.info("No authentication found");
            }
        } catch (Exception e) {
            log.error("AfterConnectionClosed : {} ", String.valueOf(e));
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textSerach) throws Exception {
        try {
            String username = getUsername(session);
            if (username != null) {
                // get username from textMessage
                String search = textSerach.getPayload();
                // check is data present correctly
                if (search.isEmpty()) return;
                // send search result to user
                sendSearchResult(session, search);
            } else {
                log.info("No authentication found");
            }
        } catch (Exception e) {
            log.error("HandleTextMessage : {} ", String.valueOf(e));
        }
    }

    public void sendSearchResult(WebSocketSession session, String search) {
        try {
            List<PublicUserDTO> users = userService.getSearchResult(search, getUsername(session));
            String textSearchResult = objectMapper.writeValueAsString(users);
            session.sendMessage(new TextMessage(textSearchResult));
        } catch (Exception e) {
            log.error("SendSearchResult : {} ", String.valueOf(e));
        }
    }

    private String getUsername(WebSocketSession session) {
        Authentication authentication = (Authentication) session.getAttributes().get("auth");
        if (authentication != null) {
            return authentication.getName();
        } else {
            return null;
        }
    }

}
