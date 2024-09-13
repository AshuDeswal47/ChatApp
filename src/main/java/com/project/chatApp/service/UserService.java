package com.project.chatApp.service;

import com.project.chatApp.dataTransferObject.PublicUserDTO;
import com.project.chatApp.dataTransferObject.UserDTO;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Lazy
    @Autowired
    private ConversationService conversationService;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void createUser(UserEntity userEntity) throws Exception {
        if(userEntity.getUsername().isEmpty() || userEntity.getUsername().isBlank()) throw new Exception("Username can't be empty.");
        if(userEntity.getPassword().isBlank() || userEntity.getPassword().isEmpty()) throw new Exception("Password can't be empty.");
        try {
            UserEntity newUserEntity = new UserEntity();
            newUserEntity.setUsername(userEntity.getUsername());
            newUserEntity.setPassword(userEntity.getPassword());
            newUserEntity.setRoles(List.of("User"));
            encryptUserPassword(newUserEntity);
            newUserEntity.setConversationIds(new ArrayList<>());
            userRepository.insert(newUserEntity);
        } catch (Exception e) {
            throw new Exception("Username already exist.");
        }
    }

    public String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public UserEntity getUser() {
        Optional<UserEntity> user = userRepository.findByUsername(getUsername());
        return user.orElse(null);
    }

    public UserEntity getUser(ObjectId userId) {
        Optional<UserEntity> user = userRepository.findById(userId);
        return user.orElse(null);
    }

    public UserEntity getUser(String username) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        return user.orElse(null);
    }

    public void addConversation(ObjectId userId, ObjectId conversationId) {
        UserEntity userEntity = getUser(userId);
        if(userEntity == null) return;
        userEntity.getConversationIds().add(conversationId);
        userRepository.save(userEntity);
    }

    public UserDTO getUserDTO() throws Exception {
        UserEntity userEntity = getUser();
        if(userEntity == null) throw new Exception("User not found.");
        UserDTO userDTO = new UserDTO();
        userDTO.setId(userEntity.getId().toHexString());
        userDTO.setUsername(userEntity.getUsername());
        userDTO.setConversations(conversationService.getAllConversationDTOs());
        return userDTO;
    }

    public PublicUserDTO getPublicUserDTO(ObjectId userId) {
        UserEntity userEntity = getUser(userId);
        if(userEntity == null) return null;
        PublicUserDTO publicUserDTO = new PublicUserDTO();
        publicUserDTO.setId(userEntity.getId().toHexString());
        publicUserDTO.setUsername(userEntity.getUsername());
        return publicUserDTO;
    }

    public PublicUserDTO getPublicUserDTO(String username) throws Exception {
        UserEntity userEntity = getUser(username);
        if(userEntity == null) throw new Exception("User not found.");
        PublicUserDTO publicUserDTO = new PublicUserDTO();
        publicUserDTO.setId(userEntity.getId().toHexString());
        publicUserDTO.setUsername(userEntity.getUsername());
        return publicUserDTO;
    }

    public void encryptUserPassword(UserEntity userEntity) {
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
    }

}
