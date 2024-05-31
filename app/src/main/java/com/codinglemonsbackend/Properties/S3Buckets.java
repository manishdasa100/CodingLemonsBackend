package com.codinglemonsbackend.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "aws.s3.buckets")
@Data
public class S3Buckets {

    private String customer;
    
}
