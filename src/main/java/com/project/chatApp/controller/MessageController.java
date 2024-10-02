package com.project.chatApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chatApp.dataTransferObject.MessageDTO;
import com.project.chatApp.entity.MessageEntity;
import com.project.chatApp.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
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

    @Autowired
    public ObjectMapper objectMapper;

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadProfilePic(@RequestParam("file") MultipartFile file, @RequestParam("message") String message) {
        if (file.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please select a file to upload");
        try {
            MessageDTO messageDTO = objectMapper.readValue(message, MessageDTO.class);
            if (messageDTO.getMessage() == null || messageDTO.getTimestamp() == null || messageDTO.getConversationId() == null || messageDTO.getSenderId() == null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please select a file to upload");
            // set message status
            messageDTO.setStatus("Sent");
            return ResponseEntity.status(HttpStatus.OK).body(messageService.addMessage(file, messageService.getMessageEntryFromMessageDTO(messageDTO)));
        } catch (Exception e) {
            log.error("Failed to upload file {}", String.valueOf(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file: " + e.getMessage());
        }
    }

}
