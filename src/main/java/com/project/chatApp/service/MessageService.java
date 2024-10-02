package com.project.chatApp.service;

import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.AttachmentEntity;
import com.project.chatApp.entity.MessageEntity;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.repository.MessageRepository;
import com.project.chatApp.repository.MessageRepositoryImpl;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    public CloudinaryService cloudinaryService;

    @Autowired
    public AttachmentService attachmentService;

    public List<MessageEntity> getMessageEntities(List<ObjectId> messageIds) {
        return messageRepository.findAllById(messageIds);
    }

    public List<MessageDTO> getMessageDTOs(List<ObjectId> messageIds) {
        if (messageIds.isEmpty()) return new ArrayList<>();
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
        messageEntity.setAttachmentId(null);
        return messageEntity;
    }

    public MessageDTO getMessageDTOFromMessageEntry(MessageEntity messageEntity, AttachmentEntity attachmentEntity) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setId(messageEntity.getId().toHexString());
        messageDTO.setConversationId(messageEntity.getConversationId().toHexString());
        messageDTO.setSenderId(messageEntity.getSenderId().toHexString());
        messageDTO.setMessage(messageEntity.getMessage());
        messageDTO.setStatus(messageEntity.getStatus());
        messageDTO.setTimestamp(messageEntity.getTimestamp());
        messageDTO.setAttachment(attachmentService.getAttachmentDTOFromAttachmentEntity(attachmentEntity));
        return messageDTO;
    }

    public MessageDTO addMessage(MultipartFile multipartFile, MessageEntity messageEntity) throws Exception {
        // upload file
        String contentType = multipartFile.getContentType();
        if(contentType == null) throw new Exception("Content type can't be null");
        int lastIndex = contentType.indexOf("/", 1) + 1;
        String url = cloudinaryService.getFileUrl(cloudinaryService.uploadFile(multipartFile, contentType.substring(0,lastIndex)));
        AttachmentEntity attachmentEntity = new AttachmentEntity();
        attachmentEntity.setUrl(url);
        attachmentEntity.setContentType(contentType);
        attachmentEntity.setSize(multipartFile.getResource().contentLength());
        // insert attachment
        attachmentEntity = attachmentService.addAttachment(attachmentEntity);
        // add attachmentId in messageEntity
        messageEntity.setAttachmentId(attachmentEntity.getId());
        // add message
        messageEntity = addMessage(messageEntity, userService.getUsername());
        return getMessageDTOFromMessageEntry(messageEntity, attachmentEntity);
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
