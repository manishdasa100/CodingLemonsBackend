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

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Timer s3UploadTimer;

    @Autowired
    private DistributionSummary s3UploadSize;

    @Autowired
    private Counter s3UploadCounter;

    public void putObject(String bucketName, String key, byte[] data){
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build();
            
            s3.putObject(objectRequest, RequestBody.fromBytes(data));
            
            // Record metrics on successful upload
            s3UploadCounter.increment();
            s3UploadSize.record(data.length);
            
            log.info("Successfully uploaded object to S3: bucket={}, key={}, size={} bytes", 
                    bucketName, key, data.length);
            
        } catch (Exception e) {
            // Track S3 upload errors
            Counter errorCounter = Counter.builder("s3.upload.errors.total")
                    .description("S3 upload errors")
                    .tag("bucket", bucketName)
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry);
            errorCounter.increment();
            
            log.error("Failed to upload object to S3: bucket={}, key={}, error={}", 
                     bucketName, key, e.getMessage(), e);
            throw e;
        } finally {
            sample.stop(s3UploadTimer);
        }
    }

    public byte[] getObject(String bucketName, String key){
        Timer downloadTimer = Timer.builder("s3.download.duration")
                .description("S3 file download duration")
                .tag("service", "s3")
                .tag("bucket", bucketName)
                .register(meterRegistry);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                        .builder()
                        .key(key)
                        .bucket(bucketName)
                        .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            
            byte[] data = objectBytes.asByteArray();

            // Track download metrics
            Counter downloadCounter = Counter.builder("s3.downloads.total")
                    .description("Total number of S3 downloads")
                    .tag("service", "s3")
                    .tag("bucket", bucketName)
                    .register(meterRegistry);
            downloadCounter.increment();
            
            DistributionSummary downloadSize = DistributionSummary.builder("s3.download.size.bytes")
                    .description("S3 download file size in bytes")
                    .tag("service", "s3")
                    .tag("bucket", bucketName)
                    .register(meterRegistry);
            downloadSize.record(data.length);
            
            log.info("Successfully downloaded object from S3: bucket={}, key={}, size={} bytes", 
                    bucketName, key, data.length);

            return data;   
            
        } catch (Exception e) {
            // Track S3 download errors
            Counter errorCounter = Counter.builder("s3.download.errors.total")
                    .description("S3 download errors")
                    .tag("bucket", bucketName)
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry);
            errorCounter.increment();
            
            log.error("Failed to download object from S3: bucket={}, key={}, error={}", 
                     bucketName, key, e.getMessage(), e);
            throw e;
        } finally {
            sample.stop(downloadTimer);
        }
    }

    public void deleteObject(String bucketName, String key) {
        Timer deleteTimer = Timer.builder("s3.delete.duration")
                .description("S3 file delete duration")
                .tag("service", "s3")
                .tag("bucket", bucketName)
                .register(meterRegistry);
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3.deleteObject(deleteObjectRequest);
            
            // Track delete metrics
            Counter deleteCounter = Counter.builder("s3.deletes.total")
                    .description("Total number of S3 deletes")
                    .tag("service", "s3")
                    .tag("bucket", bucketName)
                    .register(meterRegistry);
            deleteCounter.increment();
            
            log.info("Successfully deleted object from S3: bucket={}, key={}", bucketName, key);
            
        } catch (Exception e) {
            // Track S3 delete errors
            Counter errorCounter = Counter.builder("s3.delete.errors.total")
                    .description("S3 delete errors")
                    .tag("bucket", bucketName)
                    .tag("error_type", e.getClass().getSimpleName())
                    .register(meterRegistry);
            errorCounter.increment();
            
            log.error("Failed to delete object from S3: bucket={}, key={}, error={}", 
                     bucketName, key, e.getMessage(), e);
            throw e;
        } finally {
            sample.stop(deleteTimer);
        }
    }
}
