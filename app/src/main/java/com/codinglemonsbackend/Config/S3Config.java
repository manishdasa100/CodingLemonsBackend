package com.codinglemonsbackend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    public String getRegion(){
        return region;
    }
    
    @Bean
    public S3Client getS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
