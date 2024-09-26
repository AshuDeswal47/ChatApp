package com.project.chatApp.controller;

import com.project.chatApp.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Autowired
    ConversationService conversationService;

    @PostMapping("/create")
    public ResponseEntity<?> createConversation(@RequestBody String username) {
        try {
            return new ResponseEntity<>(conversationService.createConversation(username), HttpStatus.CREATED);
        } catch(Exception e) {
            log.error("Unable to create Conversation {}", String.valueOf(e));
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
