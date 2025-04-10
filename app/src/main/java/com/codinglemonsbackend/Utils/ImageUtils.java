package com.codinglemonsbackend.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

public class ImageUtils {

    public static final List<String> validImageExtensions = Arrays.asList("jpg", "jpeg", "png");

    public record CustomImageDimensions(int width, int height) {}

    private static final CustomImageDimensions DEFAULT_DIMENSIONS = new CustomImageDimensions(200, 200);

    public static byte[] resizeImage(MultipartFile imageFile) throws IOException {
        return resizeImage(imageFile, DEFAULT_DIMENSIONS);
    }

    public static byte[] resizeImage(MultipartFile imageFile, CustomImageDimensions dimensions) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(imageFile.getInputStream())
            .size(dimensions.width, dimensions.height)
            .outputFormat("jpg")
            .outputQuality(0.8f)
            .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
    
}
