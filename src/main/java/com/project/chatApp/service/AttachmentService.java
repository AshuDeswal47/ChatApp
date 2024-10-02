package com.project.chatApp.service;

import com.project.chatApp.dataTransferObject.AttachmentDTO;
import com.project.chatApp.entity.AttachmentEntity;
import com.project.chatApp.repository.AttachmentRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttachmentService {

    @Autowired
    public AttachmentRepository attachmentRepository;

    public AttachmentEntity addAttachment(AttachmentEntity attachmentEntity) {
        return attachmentRepository.insert(attachmentEntity);
    }

    public AttachmentEntity getAttachmentEntityFromAttachmentDTO(AttachmentDTO attachmentDTO) {
        if(attachmentDTO == null || attachmentDTO.getId()  == null) return null;
        AttachmentEntity attachmentEntity = new AttachmentEntity();
        attachmentEntity.setId(new ObjectId(attachmentDTO.getId()));
        attachmentEntity.setUrl(attachmentDTO.getUrl());
        attachmentEntity.setContentType(attachmentDTO.getContentType());
        attachmentEntity.setSize(attachmentDTO.getSize());
        return attachmentEntity;
    }

    public AttachmentDTO getAttachmentDTOFromAttachmentEntity(AttachmentEntity attachmentEntity) {
        if(attachmentEntity == null || attachmentEntity.getId() == null) return null;
        AttachmentDTO attachmentDTO = new AttachmentDTO();
        attachmentDTO.setId(attachmentEntity.getId().toHexString());
        attachmentDTO.setUrl(attachmentEntity.getUrl());
        attachmentDTO.setContentType(attachmentEntity.getContentType());
        attachmentDTO.setSize(attachmentEntity.getSize());
        return attachmentDTO;
    }

}
