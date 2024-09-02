package com.project.chatApp.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chatApp.entity.Message;
import com.project.chatApp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class messageWebSocketHandler extends TextWebSocketHandler {

    // active connections with key : username
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MessageService messageService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // get username form url when connection establish
        String username = getUsernameFromUrl(session);
        // store active connections in hashMap
        sessions.put(username, session);
        // get new messages of this user
        List<Message> messages = messageService.getNewMessages(username);
        // send these unread messages to this user
        sendMessageToClients(messages);
        // update these messages because know they received
        messageService.updateNewMessages(username, "received");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // get username form session
        String username = getUsernameFromSession(session);
        // remove the disconnected webSocket from hashMap
        sessions.remove(username);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        // convert jason string to POJO
        Message message = objectMapper.readValue(textMessage.getPayload(), Message.class);
        // send message to receiver if webSocket is connected
        if (sendMessageToClients(message)) {
            // if sendMessageToClients is successful then set message state as received
            message.setStatus("received");
        }
        // add message in mongodb-database
        messageService.addMessage(message);
    }

    public boolean sendMessageToClients(Message message) {
        try {
            // check if receiver is connected to webSocket
            if (sessions.containsKey(message.getReceiverId())) {
                // if receiver user is connected send him the message
                // convert pojo to jason string
                String textMessage = objectMapper.writeValueAsString(message);
                // send message to receiver user
                sessions.get(message.getReceiverId()).sendMessage(new TextMessage(textMessage));
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendMessageToClients(List<Message> messages) {
        if(messages.isEmpty()) return false;
        try {
            // check if receiver is connected to webSocket
            if (sessions.containsKey(messages.get(0).getSenderId())) {
                // if receiver user is connected send him the message
                // convert pojo to jason string
                String textMessage = objectMapper.writeValueAsString(messages);
                // send message to receiver user
                sessions.get(messages.get(0).getReceiverId()).sendMessage(new TextMessage(textMessage));
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getUsernameFromUrl(WebSocketSession session) {
        // Extract query parameters from the WebSocket session URL
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null) {
                Map<String, String> params = splitQuery(query);
                String username = params.get("username");
                // Store the username in session attributes if needed
                session.getAttributes().put("username", username);
                return username;
            }
        }
        return null;
    }

    private Map<String, String> splitQuery(String query) {
        // Utility method to parse query string into a Map
        return java.util.Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }

    private String getUsernameFromSession(WebSocketSession session) {
        // Implement logic to retrieve username from session attributes
        return (String) session.getAttributes().get("username");
    }

}
