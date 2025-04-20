package com.codinglemonsbackend.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

public class ImageUtils {

    public static final List<String> validImageExtensions = Arrays.asList("jpg", "jpeg", "png");

    public static final float DEFAULT_IMAGE_QUALITY = 0.9f;

    public enum ImageDimension {
        SQUARE(300, 300),
        PORTRAIT(300, 450),
        LANDSCAPE(450, 300);

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
            .outputQuality(DEFAULT_IMAGE_QUALITY)
            .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
    
}
