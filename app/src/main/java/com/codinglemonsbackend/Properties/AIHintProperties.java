package com.codinglemonsbackend.Properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@ConfigurationProperties(prefix = "ai.hint")
@Data
@Validated
public class AIHintProperties {
    @NotEmpty private List<String> providerPriority;
    @Min(1) private int dailyUserQuota;
    @Min(1) private int cooldownSeconds;
    @Min(1) private int providerTimeoutSeconds;
}
