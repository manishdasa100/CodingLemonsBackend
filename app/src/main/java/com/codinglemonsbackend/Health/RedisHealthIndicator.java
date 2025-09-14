package com.codinglemonsbackend.Health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public Health health() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            
            if (connection != null) {
                Properties info = connection.commands().info();
                String version = info.getProperty("redis_version");
                String mode = info.getProperty("redis_mode");
                String usedMemory = info.getProperty("used_memory_human");
                
                connection.close();
                
                return Health.up()
                        .withDetail("version", version)
                        .withDetail("mode", mode)
                        .withDetail("used_memory", usedMemory)
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Could not establish connection")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}