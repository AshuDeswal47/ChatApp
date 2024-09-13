package com.project.chatApp.service;

import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.ConversationEntity;
import com.project.chatApp.entity.MessageEntity;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.repository.MessageRepository;
import com.project.chatApp.repository.MessageRepositoryImpl;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    public MessageRepository messageRepository;

    @Autowired
    public MessageRepositoryImpl messageRepositoryImpl;

    @Lazy
    @Autowired
    public UserService userService;

    @Lazy
    @Autowired
    public ConversationService conversationService;

    public MessageEntity getMessageEntity(ObjectId messageId) {
        return messageRepository.findById(messageId).orElse(null);
    }

    public List<MessageEntity> getMessageEntities(List<ObjectId> messageIds) {
        return messageRepository.findAllById(messageIds);
    }

    public List<MessageDTO> getMessageDTOs(List<ObjectId> messageIds) {
        List<MessageEntity> messageEntities = getMessageEntities(messageIds);
        return messageEntities.stream().map(messageEntity -> {
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setId(messageEntity.getId().toHexString());
            messageDTO.setConversationId(messageEntity.getConversationId().toHexString());
            messageDTO.setSenderId(messageEntity.getSenderId().toHexString());
            messageDTO.setMessage(messageEntity.getMessage());
            messageDTO.setStatus(messageEntity.getStatus());
            messageDTO.setTimestamp(messageEntity.getTimestamp());
            return messageDTO;
        }).toList();
    }

    public List<MessageDTO> getLast20MessageEntitiesBeforeMessageId(String conversationId, String topMessageId) throws Exception {
        // get conversationEntity from conversationId
        ConversationEntity conversationEntity = conversationService.getConversationEntity(new ObjectId(conversationId));
        // if conversationEntity is null then throw exception
        if(conversationEntity == null) throw new Exception("Conversation not found.");
        // get messageIds from conversationIds
        List<ObjectId> messageIds = conversationEntity.getMessageIds();
        // get index of topMessageId
        int topMessageIndex = messageIds.indexOf(new ObjectId(topMessageId));
        // if topMessageId is not present in conversation list throw exception
        if(topMessageIndex == -1) throw new Exception("Message not found");
        // if input is correct extract at-most 20 messageObjectIds above topMessageId
        int nextTopMessageIndex = 0;
        if(topMessageIndex > 20) {
            nextTopMessageIndex = topMessageIndex - 20;
        }
        messageIds = messageIds.subList(nextTopMessageIndex, topMessageIndex);
        // get message DTOs from these object ids and return
        return getMessageDTOs(messageIds);
    }

    public MessageEntity getMessageEntryFromMessageDTO(MessageDTO messageDTO) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setSenderId(new ObjectId(messageDTO.getSenderId()));
        messageEntity.setConversationId(new ObjectId(messageDTO.getConversationId()));
        messageEntity.setMessage(messageDTO.getMessage());
        messageEntity.setStatus(messageDTO.getStatus());
        messageEntity.setTimestamp(messageDTO.getTimestamp());
        return messageEntity;
    }

    public MessageDTO getMessageDTOFromMessageEntry(MessageEntity messageEntity) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(messageEntity.getId().toHexString());
        messageDTO.setConversationId(messageEntity.getConversationId().toHexString());
        messageDTO.setSenderId(messageEntity.getSenderId().toHexString());
        messageDTO.setMessage(messageEntity.getMessage());
        messageDTO.setStatus(messageEntity.getStatus());
        messageDTO.setTimestamp(messageEntity.getTimestamp());
        return messageDTO;
    }

    public MessageEntity addMessage(MessageEntity messageEntity, String username) {
        UserEntity userEntity = userService.getUser(messageEntity.getSenderId());
        if (!username.equals(userEntity.getUsername())) messageEntity.setSenderId(userService.getUser(username).getId());
        // check if message sender is part of the conversation or not
        ObjectId messageEntityConversationId = messageEntity.getConversationId();
        boolean isConversationIdPresent = false;
        for(ObjectId conversationId : userEntity.getConversationIds()) {
            if(conversationId.equals(messageEntityConversationId)) isConversationIdPresent = true;
        }
        if(isConversationIdPresent) {
            MessageEntity message = messageRepository.insert(messageEntity);
            conversationService.addMessageInConversation(message.getConversationId(), message.getId());
            return message;
        }
        return null;
    }

    public void updateMessage(MessageEntity messageEntity) {
        messageRepository.save(messageEntity);
    }

    public void updateMessagesStateToReceived() {
        UserEntity userEntity = userService.getUser();
        List<ConversationEntity> conversationEntities = conversationService.getAllConversationEntities(userEntity);
        List<MessageEntity> messageEntities = new ArrayList<>();
        for(ConversationEntity conversationEntity : conversationEntities) {
            List<ObjectId> messageIds = conversationEntity.getMessageIds();
            for(int i=messageIds.size()-1; i>=0; i--) {
                MessageEntity messageEntity = getMessageEntity(messageIds.get(i));
                if(messageEntity == null || messageEntity.getSenderId().equals(userEntity.getId())) continue;
                if(!messageEntity.getStatus().equals("Sent")) break;
                messageEntity.setStatus("Received");
                messageEntities.add(messageEntity);
            }
        }
        messageRepository.saveAll(messageEntities);
    }

    public void updateMessagesStateToViewed (String conversationId) {
        UserEntity userEntity = userService.getUser();
        ConversationEntity conversationEntity = conversationService.getConversationEntity(new ObjectId(conversationId));
        List<ObjectId> messageIds = conversationEntity.getMessageIds();
        List<MessageEntity> messageEntities = new ArrayList<>();
        for(int i=messageIds.size()-1; i>=0; i--) {
            MessageEntity messageEntity = getMessageEntity(messageIds.get(i));
            if(messageEntity == null || messageEntity.getSenderId().equals(userEntity.getId())) continue;
            if(messageEntity.getStatus().equals("Viewed")) break;
            messageEntity.setStatus("Viewed");
            messageEntities.add(messageEntity);
        }
        messageRepository.saveAll(messageEntities);
    }

}
