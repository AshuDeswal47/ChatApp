package com.project.chatApp.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ImageCompressor {

    public byte[] compressAndResizeImage(MultipartFile file, int size) throws IOException {
        // Read the original image
        BufferedImage originalImage = ImageIO.read(file.getInputStream());

        // Calculate the size for center cropping
        int cropSize = Math.min(originalImage.getWidth(), originalImage.getHeight());
        int x = (originalImage.getWidth() - cropSize) / 2;
        int y = (originalImage.getHeight() - cropSize) / 2;

        // Crop the image
        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropSize, cropSize);

        // Resize the image to the desired size
        BufferedImage resizedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(croppedImage, 0, 0, size, size, null);
        g.dispose();

        // Convert ByteArrayOutputStream to MultipartFile
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpeg", baos);
        return baos.toByteArray();
    }

}
