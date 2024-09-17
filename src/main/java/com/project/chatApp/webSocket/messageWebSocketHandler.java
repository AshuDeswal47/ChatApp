package com.project.chatApp.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.MessageEntity;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.service.ConversationService;
import com.project.chatApp.service.MessageService;
import com.project.chatApp.service.UserService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageWebSocketHandler extends TextWebSocketHandler {

    // active connections with key : objectId
    private final Map<ObjectId, WebSocketSession> sessions = new ConcurrentHashMap<>();

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
                UserEntity userEntity = userService.getUser(username);
                if (userEntity != null) {
                    // store active connections in hashMap
                    sessions.put(userEntity.getId(), session);
                    System.out.println("User Connected : " + username);
                }
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
                UserEntity userEntity = userService.getUser(username);
                if (userEntity != null) {
                    // remove the disconnected webSocket from hashMap
                    sessions.remove(userEntity.getId());
                    System.out.println("User Disconnected : " + username);
                }
            } else {
                System.out.println("No Authentication Found");
            }
        } catch (Exception e) {
            // handle exception
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String username = getUsername(session);
        if (username != null) {
            // convert jason string to POJO
            MessageDTO messageDTO = objectMapper.readValue(textMessage.getPayload(), MessageDTO.class);
            // check is data present correctly
            if (messageDTO.getMessage() == null || messageDTO.getTimestamp() == null || messageDTO.getConversationId() == null || messageDTO.getSenderId() == null)
                return;
            // set message status
            messageDTO.setStatus("Sent");
            // add message in mongodb-database
            MessageEntity messageEntity = messageService.addMessage(messageService.getMessageEntryFromMessageDTO(messageDTO), username);
            // send message to receiver if messageEntity not equals to null and webSocket is connected
            if (messageEntity != null) sendMessageToClients(messageEntity);
        } else {
            System.out.println("No Authentication Found");
        }
    }

    public void sendMessageToClients(MessageEntity messageEntity) {
        try {
            // check if receiver is connected to webSocket
            ObjectId senderId = messageEntity.getSenderId();
            List<ObjectId> userIds = conversationService.getConversationEntity(messageEntity.getConversationId()).getUserIds();
            for (ObjectId userId : userIds) {
                if (userId.equals(senderId)) continue;
                if (sessions.containsKey(userId)) {
                    // if receiver user is connected send him the message
                    // convert pojo to jason string
                    String textMessage = objectMapper.writeValueAsString(messageService.getMessageDTOFromMessageEntry(messageEntity));
                    // send message to receiver user
                    sessions.get(userId).sendMessage(new TextMessage(textMessage));
                    // update status to received
                    messageEntity.setStatus("Received");
                    messageService.updateMessage(messageEntity);
                }
            }
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
