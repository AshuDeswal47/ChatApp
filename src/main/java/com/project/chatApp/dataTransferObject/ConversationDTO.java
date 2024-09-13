package com.project.chatApp.dataTransferObject;

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
public class ConversationDTO {

    private String id;

    private List<PublicUserDTO> members = new ArrayList<>();

    private List<MessageDTO> messages = new ArrayList<>();

}
