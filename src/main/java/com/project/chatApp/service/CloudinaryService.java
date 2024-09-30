package com.project.chatApp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // Upload File to Cloudinary
    public Map uploadFile(MultipartFile file) throws IOException {
        Map uploadParams = ObjectUtils.asMap(
                "transformation", new Transformation().width(500).height(500).crop("thumb")
        );
        return cloudinary.uploader().upload(file.getBytes(), uploadParams);
    }

    // Delete a File from Cloudinary by Public ID
    public Map deleteFile(String publicId) throws IOException {
        return cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // Get the URL of the uploaded image
    public String getFileUrl(Map uploadResult) {
        return uploadResult.get("url").toString();
    }

    public String getPublicId(String url) {
        // Find the last occurrence of the delimiter
        int firstIndex = url.lastIndexOf("/") + 1;
        int lastIndex = url.lastIndexOf(".");
        return url.substring(firstIndex, lastIndex);
    }

}
