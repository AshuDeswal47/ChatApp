package com.project.chatApp.controller;

import com.project.chatApp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/data")
    public ResponseEntity<?> getUserData() {
        try {
            return new ResponseEntity<>(userService.getUserDTO(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NO_CONTENT);
        }
    }

    @PostMapping("/uploadProfilePic")
    public ResponseEntity<?> uploadProfilePic(@RequestParam("profilePic") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please select a file to upload");
        try {
            userService.uploadProfilePic(file);
            return ResponseEntity.status(HttpStatus.OK).body("File uploaded successfully: " + file.getOriginalFilename());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file: " + e.getMessage());
        }
    }

    @GetMapping("/downloadProfilePic")
    public ResponseEntity<?> downloadProfilePic(@RequestParam("profilePicUrl") String profilePicUrl) {
        if (profilePicUrl.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please send profilePicUrl.");
        try {
            Resource resource = userService.downloadProfilePic(profilePicUrl);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}