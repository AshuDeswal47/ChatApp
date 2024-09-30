package com.project.chatApp.service;

import com.project.chatApp.dataTransferObject.PublicUserDTO;
import com.project.chatApp.dataTransferObject.UserDTO;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.repository.UserRepository;
import com.project.chatApp.repository.UserRepositoryImpl;
import com.project.chatApp.utils.ImageCompressor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    UserRepositoryImpl userRepositoryImpl;

    @Lazy
    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ImageCompressor imageCompressor;

    @Autowired
    private CloudinaryService cloudinaryService;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void createUser(UserEntity userEntity) throws Exception {
        if(userEntity.getUsername().isEmpty() || userEntity.getUsername().isBlank()) {
            log.error("Username can't be empty.");
            throw new Exception("Username can't be empty.");
        }
        if(userEntity.getPassword().isBlank() || userEntity.getPassword().isEmpty()) {
            log.error("Password can't be empty.");
            throw new Exception("Password can't be empty."); }
        try {
            UserEntity newUserEntity = new UserEntity();
            newUserEntity.setUsername(userEntity.getUsername());
            newUserEntity.setPassword(userEntity.getPassword());
            newUserEntity.setProfilePicUrl("Default");
            newUserEntity.setRoles(List.of("User"));
            encryptUserPassword(newUserEntity);
            newUserEntity.setConversationIds(new ArrayList<>());
            userRepository.insert(newUserEntity);
        } catch (Exception e) {
            log.error("User already exist. {}", String.valueOf(e));
            throw new Exception("User already exist.");
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

    public void addConversationInUsers(List<ObjectId> userIds, ObjectId conversationId) {
        userRepositoryImpl.addConversationIdToUsers(userIds, conversationId);
    }

    public UserDTO getUserDataDTO(UserEntity user) {
        return userRepositoryImpl.getUserDataDTO(user.getUsername());
    }

    public PublicUserDTO getPublicUserDTO(UserEntity userEntity) {
        if(userEntity == null) return null;
        PublicUserDTO publicUserDTO = new PublicUserDTO();
        publicUserDTO.setId(userEntity.getId().toHexString());
        publicUserDTO.setUsername(userEntity.getUsername());
        publicUserDTO.setProfilePicUrl(userEntity.getProfilePicUrl());
        return publicUserDTO;
    }

    public List<PublicUserDTO> getSearchResult(String search, String username) {
        return userRepository.findByUsernameStartingWith(search)
                .orElse(new ArrayList<>())
                .stream().map(userEntity -> {
                    PublicUserDTO publicUserDTO = new PublicUserDTO();
                    publicUserDTO.setId(userEntity.getId().toHexString());
                    publicUserDTO.setUsername(userEntity.getUsername());
                    publicUserDTO.setProfilePicUrl(userEntity.getProfilePicUrl());
                    return publicUserDTO;
                }).filter(userEntity -> !userEntity.getUsername().equals(username)).toList();
    }

    public String uploadProfilePic(MultipartFile file) throws Exception {
        String profilePicUrl = cloudinaryService.getFileUrl(cloudinaryService.uploadFile(file));
        if(profilePicUrl == null || profilePicUrl.isEmpty()) throw new Exception("Unable to upload profilePic");
        // Save file path in database
        UserEntity userEntity = getUser();
        // delete old profilePic from database
        if(!userEntity.getProfilePicUrl().equals("Default"))
            cloudinaryService.deleteFile(cloudinaryService.getPublicId(userEntity.getProfilePicUrl()));
        // update profilePicUrl
        userRepositoryImpl.updateProfilePicUrl(userEntity.getId(), profilePicUrl);
        return profilePicUrl;
    }

    public List<ObjectId> updateMyMessagesOfAllConversationsToReceived(ObjectId userId) {
        return userRepositoryImpl.updateMyMessagesOfAllConversationsToReceived(userId);
    }

    public void encryptUserPassword(UserEntity userEntity) {
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
    }

}
