package com.project.chatApp.controller;

import com.project.chatApp.service.ConversationService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Autowired
    ConversationService conversationService;

    @GetMapping("/getAll")
    public ResponseEntity<?> getAllConversations() {
        try {
            return new ResponseEntity<>(conversationService.getAllConversationDTOs(), HttpStatus.OK);
        } catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createConversation(@RequestBody String username) {
        try {
            return new ResponseEntity<>(conversationService.createConversation(username), HttpStatus.CREATED);
        } catch(Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
