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
@Document("attachment")
public class AttachmentEntity {

    @Id
    private ObjectId id;

    @NonNull
    private String url;

    @NonNull
    private String contentType;

    @NonNull
    private Long size;

}

