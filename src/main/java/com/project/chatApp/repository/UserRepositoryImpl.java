package com.project.chatApp.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

public class UserRepositoryImpl {

    private static final String COLLECTION = "user";

    @Autowired
    private MongoTemplate mongoTemplate;

}
