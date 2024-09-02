package com.project.chatApp.entity;

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
@Document("messages")
public class Message {

    @Id
    private ObjectId id;

    @NonNull
    private String senderId;

    @NonNull
    private String receiverId;

    @NonNull
    private Long timestamp;

    @NonNull
    private String message;

    @NonNull
    private String messageType;

    @NonNull
    private String status;

}
