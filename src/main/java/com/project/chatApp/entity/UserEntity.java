package com.project.chatApp.entity;


import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("user")
public class UserEntity {

    @Id
    private ObjectId id;

    @NonNull
    @Indexed(unique = true)
    private String username;

    @NonNull
    private String password;

    @NonNull
    private String profilePicUrl;

    @NonNull
    private List<String> roles = new ArrayList<>();

    @NonNull
    private List<ObjectId> conversationIds = new ArrayList<>();

    @Transient
    @DBRef
    private List<ConversationEntity> conversations = new ArrayList<>();

}
