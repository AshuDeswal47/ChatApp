package com.project.chatApp.controller;

import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.MessageEntity;
import com.project.chatApp.service.MessageService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/getLast20MessageBeforeMessageId")
    public ResponseEntity<?> getLast20MessagesBeforeTimestamp(@RequestBody Map<String, Object> payload) {
        try {
            // get conversationId and topMessageId from RequestBody
            if(!(payload.containsKey("conversationId") && payload.containsKey("topMessageId")))
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            List<MessageDTO> messageDTOs = messageService.getLast20MessageEntitiesBeforeMessageId((String) payload.get("conversationId"), (String) payload.get("topMessageId"));
            return new ResponseEntity<>(messageDTOs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

}
