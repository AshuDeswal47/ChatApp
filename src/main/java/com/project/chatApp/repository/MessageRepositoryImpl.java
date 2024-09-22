package com.project.chatApp.repository;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.AggregationUpdate;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;

public class MessageRepositoryImpl {

    private static final String COLLECTION = "message";

    @Autowired
    private MongoTemplate mongoTemplate;

}
