package com.project.chatApp.webSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.chatApp.dataTransferObject.ConversationDTO;
import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.ConversationEntity;
import com.project.chatApp.entity.MessageEntity;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.service.ConversationService;
import com.project.chatApp.service.MessageService;
import com.project.chatApp.service.UserService;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
                    log.info("MessageWebSocket user connected : {} ", username);
                    // send userData
                    sendUserData(userEntity);
                    // set all messages of this user received
                    List<ObjectId> updatedConversationIds = userService.updateMyMessagesOfAllConversationsToReceived(userEntity.getId());
                    sendStatusUpdateReceived(updatedConversationIds, userEntity);
                }
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
                UserEntity userEntity = userService.getUser(username);
                if (userEntity != null) {
                    // remove the disconnected webSocket from hashMap
                    sessions.remove(userEntity.getId());
                    log.info("MessageWebSocket user disconnected : {} ", username);
                }
            } else {
                log.info("No authentication found");
            }
        } catch (Exception e) {
            log.error("AfterConnectionClosed : {} ", String.valueOf(e));
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        try {
            String username = getUsername(session);
            if (username == null) {
                log.info("No authentication found");
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
        } catch (Exception e) {
            log.error("HandleTextMessage : {} ", String.valueOf(e));
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
        if (messageEntity != null) sendMessage(messageEntity);
    }

    private void handleStatusUpdate(String username, JsonObject json) {
        // get conversation id from json
        String conversationId = json.get("conversationId").getAsString();
        if (conversationId.isEmpty()) return;
        // get userEntity by username
        UserEntity userEntity = userService.getUser(username);
        if(userEntity == null) return;
        // update-status in database
        boolean isUpdated = conversationService.updateMyMessagesOfConversation(new ObjectId(conversationId), userEntity.getId());
        // if status is updated then send status update viewed to clients
        if(isUpdated) sendStatusUpdateViewed(new ObjectId(conversationId), userEntity);
    }

    private void handleGetMessagesRequest(String username, JsonObject json) throws Exception {
        // get conversation id and topMessageId from json
        String conversationId = json.get("conversationId").getAsString();
        String topMessageId = json.get("topMessageId").getAsString();
        if (conversationId.isEmpty() || topMessageId.isEmpty()) return;
        sendMessages(conversationId, username, conversationService.getLast20MessageEntitiesBeforeMessageId(conversationId, topMessageId));
    }

    private void sendMessage (MessageEntity messageEntity) {
        try {
            // sender user id
            ObjectId senderId = messageEntity.getSenderId();
            // receiver user id
            ObjectId receiverId = null;
            // userIds
            List<ObjectId> userIds = conversationService.getConversationEntity(messageEntity.getConversationId()).getUserIds();
            // check if receiver is connected to webSocket
            for (ObjectId userId : userIds) {
                if (userId.equals(senderId)) continue;
                if(sessions.containsKey(userId)) receiverId = userId;
            }
            if(receiverId != null) messageEntity.setStatus("Received");
            // convert pojo to jason string
            String message = objectMapper.writeValueAsString(messageService.getMessageDTOFromMessageEntry(messageEntity));
            // build json object
            JsonObject response = new JsonObject();
            response.addProperty("type", "message");
            response.addProperty("message", message);
            if(receiverId != null) {
                // if receiver user is connected send him the message
                sessions.get(receiverId).sendMessage(new TextMessage(response.toString()));
                // update status
                messageService.updateMessage(messageEntity);
            }
            // send message to sender user
            sessions.get(senderId).sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            log.error("SendMessage : {} ", String.valueOf(e));
        }
    }

    private void sendStatusUpdateReceived(List<ObjectId> conversationIds, UserEntity userEntity) {
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
            log.error("SendStatusUpdateReceived : {} ", String.valueOf(e));
        }
    }

    private void sendStatusUpdateViewed(ObjectId conversationId, UserEntity userEntity) {
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
            log.error("SendStatusUpdateViewed : {} ", String.valueOf(e));
        }
    }

    // send user data with last 20 messages
    private void sendUserData(UserEntity userEntity) {
        try {
            if(!sessions.containsKey(userEntity.getId())) return;
            // build json object
            JsonObject response = new JsonObject();
            response.addProperty("type", "user-data");
            response.addProperty("userData", objectMapper.writeValueAsString(userService.getUserDataDTO(userEntity)));
            // send message
            sessions.get(userEntity.getId()).sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            log.error("SendUserData : {} ", String.valueOf(e));
        }
    }

    private void sendMessages(String conversationId, String username, List<MessageDTO> messages) {
        try {
            UserEntity userEntity = userService.getUser(username);
            if(!sessions.containsKey(userEntity.getId())) return;
            // build json object
            JsonObject response = new JsonObject();
            response.addProperty("type", "messages");
            response.addProperty("conversationId", conversationId);
            response.addProperty("messages", objectMapper.writeValueAsString(messages));
            // send message
            sessions.get(userEntity.getId()).sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            log.error("SendMessage : {} ", String.valueOf(e));
        }
    }

    public void sendConversation(ConversationDTO conversationDTO, ObjectId userId) {
        try {
            if(!sessions.containsKey(userId)) return;
            // build json object
            JsonObject response = new JsonObject();
            response.addProperty("type", "conversation");
            response.addProperty("conversation", objectMapper.writeValueAsString(conversationDTO));
            // send message
            sessions.get(userId).sendMessage(new TextMessage(response.toString()));
        } catch (Exception e) {
            log.error("SendConversation : {} ", String.valueOf(e));
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
