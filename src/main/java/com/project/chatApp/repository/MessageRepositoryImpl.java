package com.project.chatApp.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public class MessageRepositoryImpl {

    private static final String COLLECTION = "message";

    @Autowired
    private MongoTemplate mongoTemplate;

    // Update documents that match the criteria
    public void updateNewMessages(String receiverId, String status) {
        // Create the query to find the documents
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverId").is(receiverId).and("status").is("sent"));

        // Create the update operation
        Update update = new Update().set("status", status);

        // Perform the update
        mongoTemplate.updateMulti(query, update, COLLECTION);
    }

}
