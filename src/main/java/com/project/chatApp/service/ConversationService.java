package com.project.chatApp.service;

import com.project.chatApp.dataTransferObject.ConversationDTO;
import com.project.chatApp.entity.ConversationEntity;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.repository.ConversationRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    @Autowired
    ConversationRepository conversationRepository;

    @Lazy
    @Autowired
    MessageService messageService;

    @Lazy
    @Autowired
    UserService userService;

    public ConversationDTO createConversation(String username) throws Exception {
        // check if user by this userId is present in our database
        UserEntity userEntity1 = userService.getUser();
        UserEntity userEntity2 = userService.getUser(username);
        // if not then throw error
        if (userEntity1 == null || userEntity2 == null) throw new Exception("User not found");
        // create userIds list
        List<ObjectId> userIds = new ArrayList<>();
        userIds.add(userEntity1.getId());
        userIds.add(userEntity2.getId());
        try {
            // find conversation with this user
            for (ConversationEntity myConversationEntity : getAllConversationEntities()) {
                if (myConversationEntity.getUserIds().stream().anyMatch(userObjectId -> userObjectId.equals(userEntity2.getId()))) {
                    // if conversation is present throw exception
                    throw new Exception("Conversation with this user already created.");
                }
            }
            // if conversation is not present then create a new one
            ConversationEntity conversationEntity = new ConversationEntity();
            conversationEntity.setUserIds(userIds);
            conversationEntity = conversationRepository.insert(conversationEntity);
            // and add conversationId in all members conversations list
            for (ObjectId userObjectId : userIds) {
                userService.addConversation(userObjectId, conversationEntity.getId());
            }
            return getConversationDTO(conversationEntity);
        } catch (Exception e) {
            throw new Exception("Unable to create conversation.");
        }
    }

    public ConversationEntity addMessageInConversation(ObjectId conversationId, ObjectId messageId) {
        ConversationEntity conversationEntity = getConversationEntity(conversationId);
        conversationEntity.getMessageIds().add(messageId);
        return conversationRepository.save(conversationEntity);
    }

    public ConversationEntity getConversationEntity(ObjectId conversationId) {
        Optional<ConversationEntity> conversation = conversationRepository.findById(conversationId);
        return conversation.orElse(null);
    }

    public List<ConversationEntity> getAllConversationEntities(UserEntity userEntity) {
        List<ObjectId> conversationIds = userEntity.getConversationIds();
        return conversationRepository.findAllById(conversationIds);
    }

    public List<ConversationEntity> getAllConversationEntities() {
        List<ObjectId> conversationIds = userService.getUser().getConversationIds();
        return conversationRepository.findAllById(conversationIds);
    }

    public ConversationDTO getConversationDTO(ObjectId conversationId) {
        String myUsername = userService.getUsername();
        ConversationEntity conversationEntity = getConversationEntity(conversationId);
        ConversationDTO conversationDTO = new ConversationDTO();
        conversationDTO.setId(conversationEntity.getId().toHexString());
        // get members of conversation excluding me
        conversationDTO.setMembers(conversationEntity.getUserIds().stream().map(userId -> userService.getPublicUserDTO(userId))
                .filter(publicUserDTO -> !(publicUserDTO == null || publicUserDTO.getUsername().equals(myUsername))).toList());
        List<ObjectId> messageIds = conversationEntity.getMessageIds();
        // get last 20 messages
        if (messageIds.size() > 20) {
            messageIds = messageIds.subList(messageIds.size() - 20, messageIds.size());
        }
        conversationDTO.setMessages(messageService.getMessageDTOs(messageIds));
        return conversationDTO;
    }

    public ConversationDTO getConversationDTO(ConversationEntity conversationEntity) {
        String myUsername = userService.getUsername();
        ConversationDTO conversationDTO = new ConversationDTO();
        conversationDTO.setId(conversationEntity.getId().toHexString());
        // get members of conversation excluding me
        conversationDTO.setMembers(conversationEntity.getUserIds().stream().map(userId -> userService.getPublicUserDTO(userId))
                .filter(publicUserDTO -> !(publicUserDTO == null || publicUserDTO.getUsername().equals(myUsername))).toList());
        List<ObjectId> messageIds = conversationEntity.getMessageIds();
        // get last 20 messages
        if (messageIds.size() > 20) {
            messageIds = messageIds.subList(messageIds.size() - 20, messageIds.size());
        }
        conversationDTO.setMessages(messageService.getMessageDTOs(messageIds));
        return conversationDTO;
    }

    public List<ConversationDTO> getAllConversationDTOs() {
        String myUsername = userService.getUsername();
        List<ConversationEntity> conversationEntities = getAllConversationEntities();
        return conversationEntities.stream().map(conversationEntity -> {
            ConversationDTO conversationDTO = new ConversationDTO();
            conversationDTO.setId(conversationEntity.getId().toHexString());
            // get members of conversation excluding me
            conversationDTO.setMembers(conversationEntity.getUserIds().stream().map(userId -> userService.getPublicUserDTO(userId))
                    .filter(publicUserDTO -> !(publicUserDTO == null || publicUserDTO.getUsername().equals(myUsername))).toList());
            List<ObjectId> messageIds = conversationEntity.getMessageIds();
            // get last 20 messages
            if (messageIds.size() > 20) {
                messageIds = messageIds.subList(messageIds.size() - 20, messageIds.size());
            }
            conversationDTO.setMessages(messageService.getMessageDTOs(messageIds));
            return conversationDTO;
        }).toList();
    }

}
