package com.project.chatApp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("message")
public class MessageEntity implements Comparable {

    @Id
    private ObjectId id;

    @NonNull
    private ObjectId conversationId;

    @NonNull
    private ObjectId senderId;

    @NonNull
    private String message;

    @NonNull
    private String status;

    @NonNull
    private Long timestamp;

    @Override
    public int compareTo(Object o) {
        MessageEntity that = (MessageEntity) o;
        // Compare based on the value field
        return Long.compare(this.timestamp, that.timestamp);
    }
}
