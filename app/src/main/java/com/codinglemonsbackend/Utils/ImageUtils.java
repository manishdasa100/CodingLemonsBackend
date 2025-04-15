package com.codinglemonsbackend.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

public class ImageUtils {

    public static final List<String> validImageExtensions = Arrays.asList("jpg", "jpeg", "png");

    public enum ImageDimension {
        SQUARE(200, 200),
        PORTRAIT(200, 300),
        LANDSCAPE(300, 200);

        private int width;
        private int height;

        ImageDimension(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }
        public int getHeight() {
            return height;
        }
    }

    public static byte[] resizeImage(MultipartFile imageFile, ImageDimension imageDimension) throws IOException {
        return resizeImage(imageFile, imageDimension.getWidth(), imageDimension.getHeight());
    }

    public static byte[] resizeImage(MultipartFile imageFile, int width, int height) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(imageFile.getInputStream())
            .size(width, height)
            .outputFormat("jpg")
            .outputQuality(0.8f)
            .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
    
}
