package com.codinglemonsbackend.Repository;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Dto.AuthProvider;
import com.codinglemonsbackend.Entities.UserEntity;
import com.mongodb.client.result.UpdateResult;

@Repository
public class UserRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<UserEntity> findUserByUsername(String username){
        if (username == null) return Optional.empty();
        UserEntity user = mongoTemplate.findById(username, UserEntity.class, "Users");
        return Optional.ofNullable(user);
    }

    public Optional<UserEntity> findUserByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        UserEntity user = mongoTemplate.findOne(query, UserEntity.class, "Users");
        return Optional.ofNullable(user);
    }

    public void saveUser(UserEntity user){
        mongoTemplate.save(user, "Users");
    }

    public UpdateResult resetUserPassword(String username, String newPassword){
        
        Query query = new Query(Criteria.where("username").is(username));

        Update update = new Update();

        update.set("password", newPassword);

        update.set("passwordIssueDate", new Date((System.currentTimeMillis() / 1000) * 1000));

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, UserEntity.class);

        return updateResult;
    }

    public void updateUserDetails(String username, Map<String, Object> updatePropertiesMap) {
        
        Query query = new Query(Criteria.where("username").is(username));

        Update update = new Update();

        updatePropertiesMap.entrySet().stream().forEach(e -> {
            update.set(e.getKey(), e.getValue());
        });

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, UserEntity.class);
    }

    public Optional<UserEntity> getUserbyAuthProviderIdAndProviderType(String id, AuthProvider provider) {
        Query query = new Query(Criteria.where("authProviderId").is(id).and("authProvider").is(provider));
        UserEntity user = mongoTemplate.findOne(query, UserEntity.class, "Users");
        return Optional.ofNullable(user);
    }

    /*public void updateUserProfilePictureId(String username, String profilePictureId) {

        Query query = new Query(Criteria.where("username").is(username));

        Update update = new Update();

        if (profilePictureId != null) { 
            update.set("profilePictureId", profilePictureId);
            mongoTemplate.updateFirst(query, update, UserEntity.class);
        } else {
            throw new IllegalArgumentException("profilePictureId cannot be null");
        }

        // if (!update.getUpdateObject().isEmpty()) {
        //     System.out.println("UPDATING PROFILE PICTURE ID");
        //     mongoTemplate.updateFirst(query, update, UserEntity.class);
        // }
    }
    */
}
