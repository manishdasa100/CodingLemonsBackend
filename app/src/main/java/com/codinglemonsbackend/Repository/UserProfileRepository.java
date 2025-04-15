package com.codinglemonsbackend.Repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.mongodb.client.result.UpdateResult;

@Repository
public class UserProfileRepository {
    
    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<UserProfileEntity> getUserProfile(String username) {
        UserProfileEntity userProfileEntity = mongoTemplate.findById(username, UserProfileEntity.class);
        return Optional.ofNullable(userProfileEntity);
    }

    public void saveUserProfile(UserProfileEntity userProfileEntity) {
        mongoTemplate.save(userProfileEntity, "UserProfile");
        System.out.println("User profile saved");
    }

    // public void updateUserProfilePictureId(String username, String profilePictureId) {

    //     Query query = new Query(Criteria.where("username").is(username));

    //     Update update = new Update();

    //     if (profilePictureId != null) { 
    //         System.out.println("UPDATING PROFILE PICTURE ID");
    //         update.set("profilePictureId", profilePictureId);
    //         mongoTemplate.updateFirst(query, update, UserProfileEntity.class);
    //     } else {
    //         throw new IllegalArgumentException("profilePictureId cannot be null");
    //     }

    //     // if (!update.getUpdateObject().isEmpty()) {
    //     //     System.out.println("UPDATING PROFILE PICTURE ID");
    //     //     mongoTemplate.updateFirst(query, update, UserEntity.class);
    //     // }
    // }

    public boolean updateUserProfile(String username, Map<String, Object> updatePropertiesMap) {
       
        Query query = new Query(Criteria.where("username").is(username));

        Update update = new Update();

        updatePropertiesMap.entrySet().stream().forEach(e -> {
            update.set(e.getKey(), e.getValue());
        });

        UpdateResult result = mongoTemplate.updateFirst(query, update, UserProfileEntity.class);
        
        if (result.getModifiedCount() > 0) return true;

        return false;
    }
}
