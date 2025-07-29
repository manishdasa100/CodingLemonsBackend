package com.codinglemonsbackend.Utils;

import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.springframework.web.multipart.MultipartFile;


public class ImageUtils {

    public static final List<String> validImageUploadExtensions = Arrays.asList("jpg", "jpeg", "png");

    public static final float DEFAULT_IMAGE_QUALITY = 0.9f;

    public enum ImageDimension {
        SQUARE(300, 300),
        SQUARE_SMALL(200, 200),
        PORTRAIT(300, 450),
        PORTRAIT_SMALL(200, 300),
        LANDSCAPE(450, 300),
        LANDSCAPE_SMALL(300, 200),;

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

        // TODO: Exception logging required in this function

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty");
        }

        if (height <= 0 || width <= 0) {
            throw new IllegalArgumentException("Height and width of the image should be greater than 0");
        }

        BufferedImage srcImage = null;
        BufferedImage resizedImage = null;
        ByteArrayOutputStream baos = null;
        try{
            srcImage = ImageIO.read(imageFile.getInputStream());
            resizedImage = Scalr.resize(srcImage, width, height, Scalr.OP_ANTIALIAS);
            baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "PNG", baos);
            return baos.toByteArray();
        }
        catch(IOException | ImagingOpException e){
            throw new IOException("Failed to read image file", e);
        }
        finally{
            if(imageFile != null){
                imageFile.getInputStream().close();
            }
            if(baos != null){
                baos.close();
            }
            if(srcImage != null){
                srcImage.flush();
                srcImage.getGraphics().dispose();
            }
            if(resizedImage != null){
                resizedImage.flush();
                resizedImage.getGraphics().dispose();
            }
        }

    }

    public static String getAssetUrl(String assetId) {
        return null;
    }
    
}
