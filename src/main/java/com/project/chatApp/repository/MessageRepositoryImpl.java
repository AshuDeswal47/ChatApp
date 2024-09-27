package com.project.chatApp.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MessageRepositoryImpl {

    private static final String COLLECTION = "message";

    @Autowired
    private MongoTemplate mongoTemplate;

}
