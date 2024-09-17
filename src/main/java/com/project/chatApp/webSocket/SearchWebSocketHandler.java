package com.project.chatApp.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chatApp.dataTransferObject.PublicUserDTO;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.service.ConversationService;
import com.project.chatApp.service.MessageService;
import com.project.chatApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                System.out.println("User Connected : " + username);
            } else {
                System.out.println("No Authentication Found");
            }
        } catch (Exception e) {
            // handle exception
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            String username = getUsername(session);
            if (username != null) {
                // remove the disconnected webSocket from hashMap
                sessions.remove(username);
                System.out.println("User Disconnected : " + username);
            } else {
                System.out.println("No Authentication Found");
            }
        } catch (Exception e) {
            // handle exception
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textSerach) throws Exception {
        String username = getUsername(session);
        if (username != null) {
            // get username from textMessage
            String search = textSerach.getPayload();
            // check is data present correctly
            if (search.isEmpty()) return;
            // send search result to user
            sendSearchResult(session, search);
        } else {
            System.out.println("No Authentication Found");
        }
    }

    public void sendSearchResult(WebSocketSession session, String search) {
        try {
            List<PublicUserDTO> users = userService.getPublicUserDTOs(search);
            String textSearchResult = objectMapper.writeValueAsString(users);
            session.sendMessage(new TextMessage(textSearchResult));
        } catch (Exception e) {
            e.printStackTrace();
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
