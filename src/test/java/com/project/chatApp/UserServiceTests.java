package com.project.chatApp;

import com.project.chatApp.dataTransferObject.PublicUserDTO;
import com.project.chatApp.dataTransferObject.UserDTO;
import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.service.UserService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class UserServiceTests {

    @Autowired
    UserService userService;

    @Test
    public void testGetUserByUsername() {
        assertNotNull(userService.getUser("ashu"));
    }

    @ParameterizedTest
    @CsvSource({
            "'66eb9533b0a04e05ca4a77f7', 'ashu'",
            "'66eb954bb0a04e05ca4a77f8', 'vijay'",
            "'66ebe93ebc2f3f2eb5e64afb', 'ajay'"
    })
    public void testGetUserByObjectId(String objectId, String expectedUsername) {
        UserEntity userEntity = userService.getUser(new ObjectId(objectId));
        assertNotNull(userEntity);
        assertEquals(expectedUsername, userEntity.getUsername());
    }

    @ParameterizedTest
    @ValueSource(strings={"66eb9533b0a04e05ca4a77f7", "66eb954bb0a04e05ca4a77f8", "66ebe93ebc2f3f2eb5e64afb"})
    public void testGetUserDataDTO(String objectId) {
        UserDTO userDataDTO = userService.getUserDataDTO(userService.getUser(new ObjectId(objectId)));
        assertNotNull(userDataDTO);
        assertNotNull(userDataDTO.getId());
        assertNotNull(userDataDTO.getUsername());
        assertNotNull(userDataDTO.getProfilePicUrl());
        assertNotNull(userDataDTO.getConversations());
    }

    @ParameterizedTest
    @ValueSource(strings={"66eb9533b0a04e05ca4a77f7", "66eb954bb0a04e05ca4a77f8", "66ebe93ebc2f3f2eb5e64afb"})
    public void testGetPublicUserDTO(String objectId) {
        PublicUserDTO publicUserDTO = userService.getPublicUserDTO(userService.getUser(new ObjectId(objectId)));
        assertNotNull(publicUserDTO);
        assertNotNull(publicUserDTO.getId());
        assertNotNull(publicUserDTO.getUsername());
        assertNotNull(publicUserDTO.getProfilePicUrl());
    }

    @ParameterizedTest
    @CsvSource({
            "'aj', 'ashu'",
            "'v', 'vijay'",
            "'m', 'ajay'"
    })
    public void testGetSearchResult(String search, String username) {
        List<PublicUserDTO> publicUserDTOs = userService.getSearchResult(search, username);
        assertNotNull(publicUserDTOs);
        for(PublicUserDTO publicUserDTO : publicUserDTOs) {
            assertNotNull(publicUserDTO.getId());
            assertNotNull(publicUserDTO.getUsername());
            assertNotNull(publicUserDTO.getProfilePicUrl());
        }
    }

}
