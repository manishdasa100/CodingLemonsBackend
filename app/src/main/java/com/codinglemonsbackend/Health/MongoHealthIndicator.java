package com.codinglemonsbackend.Health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoHealthIndicator implements HealthIndicator {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Health health() {
        try {
            String version = mongoTemplate.getDb().runCommand(new org.bson.Document("buildInfo", 1))
                    .getString("version");
            long collectionsCount = mongoTemplate.getCollectionNames().size();
            
            return Health.up()
                    .withDetail("database", mongoTemplate.getDb().getName())
                    .withDetail("version", version)
                    .withDetail("collections", collectionsCount)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}