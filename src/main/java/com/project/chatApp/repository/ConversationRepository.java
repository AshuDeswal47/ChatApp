package com.project.chatApp.repository;

import com.project.chatApp.entity.ConversationEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConversationRepository extends MongoRepository<ConversationEntity, ObjectId> {

}
