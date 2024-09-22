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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public MessageEntity addMessage(MessageEntity messageEntity, String username) throws Exception {
            UserEntity userEntity = userService.getUser(messageEntity.getSenderId());
            if (!username.equals(userEntity.getUsername()))
                messageEntity.setSenderId(userService.getUser(username).getId());
            // check if message sender is part of the conversation or not
            ObjectId messageEntityConversationId = messageEntity.getConversationId();
            boolean isConversationIdPresent = false;
            for (ObjectId conversationId : userEntity.getConversationIds()) {
                if (conversationId.equals(messageEntityConversationId)) isConversationIdPresent = true;
            }
            if (isConversationIdPresent) {
                MessageEntity message = messageRepository.insert(messageEntity);
                conversationService.addMessageInConversation(message.getConversationId(), message.getId());
                return message;
            }
            return null;
    }

    public void updateMessage(MessageEntity messageEntity) {
        messageRepository.save(messageEntity);
    }

}
