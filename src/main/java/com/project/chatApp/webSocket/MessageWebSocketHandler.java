package com.project.chatApp.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.ConversationEntity;
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
                    System.out.println("Message WebSocket User Connected : " + username);
                    // set all messages of this user received
                    List<ObjectId> updatedConversationIds = messageService.updateMessagesStateToReceived(username);
                    sendStatusUpdateReceivedToClients(updatedConversationIds, userEntity);
                    // send userData
                    sendUserData(userEntity);
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
                    System.out.println("Message WebSocket User Disconnected : " + username);
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
        if (username == null) {
            System.out.println("No Authentication Found");
            return;
        }
        // convert json string to jsonObject
        String payload = textMessage.getPayload();
        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
        String type = json.get("type").getAsString();
        // manage data according to data type
        if (type.equals("message")) {
            // new message
            handleTextNewMessage(username, json);
        } else if (type.equals("status-update")) {
            // messages status-update
            handleStatusUpdate(username, json);
        } else if (type.equals("get-messages")) {
            // get request for previous 20 messages of specific conversation
            handleGetMessagesRequest(username, json);
        }
    }

    private void handleTextNewMessage(String username, JsonObject json) throws Exception {
        // get actual message from json
        String message = json.get("message").getAsString();
        if (message.isEmpty()) return;
        // convert jason string to POJO
        MessageDTO messageDTO = objectMapper.readValue(message, MessageDTO.class);
        // check is data present correctly
        if (messageDTO.getMessage() == null || messageDTO.getTimestamp() == null || messageDTO.getConversationId() == null || messageDTO.getSenderId() == null)
            return;
        // set message status
        messageDTO.setStatus("Sent");
        // add message in mongodb-database
        MessageEntity messageEntity = messageService.addMessage(messageService.getMessageEntryFromMessageDTO(messageDTO), username);
        // send message to receiver if messageEntity not equals to null and webSocket is connected
        if (messageEntity != null) sendMessageToClients(messageEntity);
    }

    private void handleStatusUpdate(String username, JsonObject json) {
        // get conversation id from json
        String conversationId = json.get("conversationId").getAsString();
        if (conversationId.isEmpty()) return;
        // get userEntity by username
        UserEntity userEntity = userService.getUser(username);
        if(userEntity == null) return;
        // update-status in database
        boolean isUpdated = messageService.updateMessagesStateToViewed(conversationId);
        // if status is updated then send status update viewed to clients
        if (isUpdated) sendStatusUpdateViewedToClients(new ObjectId(conversationId), userEntity);
    }

    private void handleGetMessagesRequest(String username, JsonObject json) throws Exception {
        // get conversation id and topMessageId from json
        String conversationId = json.get("conversationId").getAsString();
        String topMessageId = json.get("topMessageId").getAsString();
        if (conversationId.isEmpty() || topMessageId.isEmpty()) return;
        sendMessages(conversationId, username, messageService.getLast20MessageEntitiesBeforeMessageId(conversationId, topMessageId));
    }

    private void sendMessageToClients(MessageEntity messageEntity) {
        try {
            // sender user id
            ObjectId userObjectId = messageEntity.getSenderId();
            // build json object
            JsonObject response = new JsonObject();
            // userIds
            List<ObjectId> userIds = conversationService.getConversationEntity(messageEntity.getConversationId()).getUserIds();
            // check if receiver is connected to webSocket
            for (ObjectId userId : userIds) {
                if (userId.equals(userObjectId)) continue;
                if (sessions.containsKey(userId)) {
                    // convert pojo to jason string
                    messageEntity.setStatus("Received");
                    String message = objectMapper.writeValueAsString(messageService.getMessageDTOFromMessageEntry(messageEntity));
                    // set properties in json
                    response.addProperty("type", "message");
                    response.addProperty("message", message);
                    // if receiver user is connected send him the message
                    sessions.get(userId).sendMessage(new TextMessage(response.toString()));
                    // update status
                    messageService.updateMessage(messageEntity);
                } else {
                    // convert pojo to jason string
                    messageEntity.setStatus("Sent");
                    String message = objectMapper.writeValueAsString(messageService.getMessageDTOFromMessageEntry(messageEntity));
                    // set properties in json
                    response.addProperty("type", "message");
                    response.addProperty("message", message);
                }
            }
            // send message to sender user
            sessions.get(userObjectId).sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendStatusUpdateReceivedToClients(List<ObjectId> conversationIds, UserEntity userEntity) {
        try {
            for (ConversationEntity conversationEntity : conversationService.getAllConversationEntities(conversationIds)) {
                for (ObjectId userId : conversationEntity.getUserIds()) {
                    if (userId.equals(userEntity.getId())) continue;
                    if (sessions.containsKey(userId)) {
                        // build json object
                        JsonObject response = new JsonObject();
                        response.addProperty("type", "status-update");
                        response.addProperty("conversationId", conversationEntity.getId().toHexString());
                        response.addProperty("status", "Received");
                        // send message
                        sessions.get(userId).sendMessage(new TextMessage(response.toString()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendStatusUpdateViewedToClients(ObjectId conversationId, UserEntity userEntity) {
        try {
            ConversationEntity conversationEntity = conversationService.getConversationEntity(conversationId);
            for (ObjectId userId : conversationEntity.getUserIds()) {
                if (userId.equals(userEntity.getId())) continue;
                if (sessions.containsKey(userId)) {
                    // build json object
                    JsonObject response = new JsonObject();
                    response.addProperty("type", "status-update");
                    response.addProperty("conversationId", conversationEntity.getId().toHexString());
                    response.addProperty("status", "Viewed");
                    // send message
                    sessions.get(userId).sendMessage(new TextMessage(response.toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // send user data with last 20 messages
    private void sendUserData(UserEntity userEntity) {
        try {
            // build json object
            JsonObject response = new JsonObject();
            response.addProperty("type", "user-data");
            response.addProperty("userData", objectMapper.writeValueAsString(userService.getUserDTO(userEntity)));
            // send message
            sessions.get(userEntity.getId()).sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessages(String conversationId, String username, List<MessageDTO> messages) {
        try {
            UserEntity userEntity = userService.getUser(username);
            // build json object
            JsonObject response = new JsonObject();
            response.addProperty("type", "messages");
            response.addProperty("conversationId", conversationId);
            response.addProperty("messages", objectMapper.writeValueAsString(messages));
            // send message
            sessions.get(userEntity.getId()).sendMessage(new TextMessage(response.toString()));
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
