package com.project.chatApp.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("conversation")
public class ConversationEntity {

    @Id
    private ObjectId id;

    @NonNull
    private List<ObjectId> userIds = new ArrayList<>();

    @NonNull
    private List<ObjectId> messageIds = new ArrayList<>();

}
