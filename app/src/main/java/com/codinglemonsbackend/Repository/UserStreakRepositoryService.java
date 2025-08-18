package com.codinglemonsbackend.Repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserStreakEntity;

@Repository
public class UserStreakRepositoryService {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<UserStreakEntity> getUserStreak(String username) {
        Query query = new Query(Criteria.where("username").is(username));
        UserStreakEntity streakEntity = mongoTemplate.findOne(query, UserStreakEntity.class);
        return Optional.ofNullable(streakEntity);
    }

    public void saveUserStreak(UserStreakEntity entity) {
        mongoTemplate.save(entity);
    }
}
