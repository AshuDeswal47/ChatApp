package com.project.chatApp.repository;

import com.project.chatApp.entity.Message;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, ObjectId> {

    @Query("{ $or: [ { $and: [ { 'senderId': ?0 }, { 'receiverId': ?1 } ] }, { $and: [ { 'senderId': ?1 }, { 'receiverId': ?0 } ] } ] }")
    List<Message> getMessagesOfUsersChat(String senderId, String receiverId);

    @Query("{ $and: [ { 'receiverId': ?0 }, { 'status': 'sent' } ] }")
    List<Message> getNewMessages(String receiverId);

}
