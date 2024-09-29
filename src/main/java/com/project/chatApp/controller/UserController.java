package com.project.chatApp.controller;

import com.project.chatApp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/uploadProfilePic")
    public ResponseEntity<?> uploadProfilePic(@RequestParam("profilePic") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please select a file to upload");
        try {
            return ResponseEntity.status(HttpStatus.OK).body(userService.uploadProfilePic(file));
        } catch (Exception e) {
            log.error("Failed to upload profilePic {}", String.valueOf(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file: " + e.getMessage());
        }
    }

}