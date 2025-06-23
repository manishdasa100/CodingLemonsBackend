package com.codinglemonsbackend.Properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3Properties {
    private String region;
    private String bucket;
}
