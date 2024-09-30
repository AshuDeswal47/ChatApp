package com.project.chatApp.service;

import com.project.chatApp.dataTransferObject.ConversationDTO;
import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.ConversationEntity;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.repository.ConversationRepository;
import com.project.chatApp.repository.ConversationRepositoryImpl;
import com.project.chatApp.webSocket.MessageWebSocketConfig;
import com.project.chatApp.webSocket.MessageWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ConversationService {

    @Autowired
    ConversationRepository conversationRepository;

    @Autowired
    ConversationRepositoryImpl conversationRepositoryImpl;

    @Lazy
    @Autowired
    MessageService messageService;

    @Lazy
    @Autowired
    UserService userService;

    MessageWebSocketHandler messageWebSocketHandler;

    @Autowired
    public ConversationService(MessageWebSocketHandler messageWebSocketHandler) {
        this.messageWebSocketHandler = messageWebSocketHandler;
    }

    @Transactional
    public ConversationDTO createConversation(String username) throws Exception {
        try {
            // check if user by this userId is present in our database
            UserEntity userEntity1 = userService.getUser();
            UserEntity userEntity2 = userService.getUser(username);
            // if not then throw error
            if (userEntity1 == null || userEntity2 == null) throw new Exception("User not found");
            // create userIds list
            List<ObjectId> userIds = new ArrayList<>();
            userIds.add(userEntity1.getId());
            userIds.add(userEntity2.getId());
            // find conversation with this user
            for (ConversationEntity myConversationEntity : getAllConversationEntities()) {
                if (myConversationEntity.getUserIds().stream().anyMatch(userObjectId -> userObjectId.equals(userEntity2.getId()))) {
                    // if conversation is present throw exception
                    log.error("Conversation with this user already created.");
                    throw new Exception("Conversation with this user already created.");
                }
            }
            // if conversation is not present then create a new one
            ConversationEntity conversationEntity = new ConversationEntity();
            conversationEntity.setUserIds(userIds);
            conversationEntity = conversationRepository.insert(conversationEntity);
            // and add conversationId in all members conversations list
            userService.addConversationInUsers(userIds, conversationEntity.getId());
            // send new conversation to clients
            ConversationDTO conversationDTO = getConversationDTO(conversationEntity, userEntity2);
            messageWebSocketHandler.sendConversation(conversationDTO, userEntity2.getId());
            return conversationDTO;
        } catch (Exception e) {
            log.error("Unable to create conversation.");
            throw new Exception("Unable to create conversation.");
        }
    }

    public void addMessageInConversation(ObjectId conversationId, ObjectId messageId) throws Exception {
        if(!conversationRepositoryImpl.addMessageInConversation(conversationId, messageId))
            throw new Exception("Unable to add messageId into conversation.");
    }

    public ConversationEntity getConversationEntity(ObjectId conversationId) {
        Optional<ConversationEntity> conversation = conversationRepository.findById(conversationId);
        return conversation.orElse(null);
    }

    public List<ConversationEntity> getAllConversationEntities(UserEntity userEntity) {
        List<ObjectId> conversationIds = userEntity.getConversationIds();
        return conversationRepository.findAllById(conversationIds);
    }

    public List<ConversationEntity> getAllConversationEntities(List<ObjectId> conversationIds) {
        return conversationRepository.findAllById(conversationIds);
    }

    public List<ConversationEntity> getAllConversationEntities() {
        List<ObjectId> conversationIds = userService.getUser().getConversationIds();
        return conversationRepository.findAllById(conversationIds);
    }

    public ConversationDTO getConversationDTO(ConversationEntity conversationEntity, UserEntity member) {
        ConversationDTO conversationDTO = new ConversationDTO();
        conversationDTO.setId(conversationEntity.getId().toHexString());
        // get members of conversation excluding me
        conversationDTO.setMembers(List.of(userService.getPublicUserDTO(member)));
        List<ObjectId> messageIds = conversationEntity.getMessageIds();
        // get last 20 messages
        if (messageIds.size() > 20) {
            messageIds = messageIds.subList(messageIds.size() - 20, messageIds.size());
        }
        conversationDTO.setMessages(messageService.getMessageDTOs(messageIds));
        return conversationDTO;
    }

    public List<MessageDTO> getLast20MessageEntitiesBeforeMessageId(String conversationId, String topMessageId) {
        return conversationRepositoryImpl.getLast20MessageEntitiesBeforeMessageId(new ObjectId(conversationId), new ObjectId(topMessageId));
    }

    public boolean updateMyMessagesOfConversation(ObjectId conversationId, ObjectId userId) {
        return conversationRepositoryImpl.updateMyMessagesOfConversationToViewed(conversationId, userId);
    }

}
