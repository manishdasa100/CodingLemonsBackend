package com.codinglemonsbackend.Service;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class S3Service {

    @Autowired
    private S3Client s3;

    public void putObject(String bucketName, String key, byte[] data){
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build();
            
            s3.putObject(objectRequest, RequestBody.fromBytes(data));
            
            log.info("Successfully uploaded object to S3: bucket={}, key={}, size={} bytes", 
                    bucketName, key, data.length);
            
        } catch (Exception e) {
            log.error("Failed to upload object to S3: bucket={}, key={}, error={}", 
                     bucketName, key, e.getMessage(), e);
            throw e;
        } 
    }

    public byte[] getObject(String bucketName, String key){
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                        .builder()
                        .key(key)
                        .bucket(bucketName)
                        .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();
            
            log.info("Successfully downloaded object from S3: bucket={}, key={}, size={} bytes", 
                    bucketName, key, data.length);
            return data;      
        } catch (Exception e) {
            log.error("Failed to download object from S3: bucket={}, key={}, error={}", 
                     bucketName, key, e.getMessage(), e);
            throw e;
        }
    }

    public void deleteObject(String bucketName, String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3.deleteObject(deleteObjectRequest);
            
            log.info("Successfully deleted object from S3: bucket={}, key={}", bucketName, key);
            
        } catch (Exception e) {
            log.error("Failed to delete object from S3: bucket={}, key={}, error={}", 
                     bucketName, key, e.getMessage(), e);
            throw e;
        }
    }
}
