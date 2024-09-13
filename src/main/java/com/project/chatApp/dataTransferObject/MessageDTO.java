package com.project.chatApp.dataTransferObject;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    private String id;

    private String conversationId;

    private String senderId;

    private String message;

    private String status;

    private Long timestamp;

}
