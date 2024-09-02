package com.project.chatApp.controller;

import com.project.chatApp.entity.Message;
import com.project.chatApp.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// It is a specialized version of the @Controller annotation, designed to handle RESTful web services.
// It is used to create RRESTful endpoints in a Spring application.
// it is combination of @Controller and @ResponseBody annotations.
// When we use @RestController Spring automatically serializes the return value of the methods into JSON or XML, based on the client's request.
// And also create been of this class.
@RestController
// We use @RequestMapping to specify the URL pattern that should be mapped to a particular controller method.
@RequestMapping("/message")
public class MessageController {

    @Autowired
    public MessageService messageService;

    @GetMapping("/senderId/{senderId}/receiverId/{receiverId}")
    public ResponseEntity<List<Message>> getAllMessage(@PathVariable String senderId,@PathVariable String receiverId) {
        List<Message> messages = messageService.getAllMessages(senderId, receiverId);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

    @GetMapping("/receiverId/{receiverId}")
    public ResponseEntity<List<Message>> getNewMessage(@PathVariable String receiverId) {
        List<Message> messages = messageService.getNewMessages(receiverId);
        return new ResponseEntity<>(messages, HttpStatus.OK);
    }

}
