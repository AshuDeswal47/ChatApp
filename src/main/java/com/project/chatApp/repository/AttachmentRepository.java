package com.project.chatApp.repository;

import com.project.chatApp.entity.AttachmentEntity;
import com.project.chatApp.entity.ConversationEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AttachmentRepository extends MongoRepository<AttachmentEntity, ObjectId> {

}
