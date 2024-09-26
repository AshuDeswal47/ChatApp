package com.project.chatApp.controller;


import com.project.chatApp.entity.UserEntity;
import com.project.chatApp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserEntity userEntity) {
        try {
            userService.createUser(userEntity);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch(Exception e) {
            log.error("Unable to SignUp {}", String.valueOf(e));
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
