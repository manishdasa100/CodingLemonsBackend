package com.codinglemonsbackend.Repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserEntity;
import com.mongodb.client.result.UpdateResult;

@Repository
public class UserRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Optional<UserEntity> getUser(String username){
        System.out.println("Username: "+ username);
        UserEntity user = mongoTemplate.findById(username, UserEntity.class, "Users");
        return Optional.ofNullable(user);
    }

    public void saveUser(UserEntity user){
        mongoTemplate.save(user, "Users");
    }

    public UpdateResult resetUserPassword(String username, String newPassword){
        
        Query query = new Query(Criteria.where("username").is(username));

        Update update = new Update();

        update.set("password", newPassword);

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, UserEntity.class);

        return updateResult;
    }

    public boolean updateUserDetails(String username, Map<String, Object> updatePropertiesMap) {
        
        Query query = new Query(Criteria.where("username").is(username));

        Update update = new Update();
        
        /*if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().equals(user.getFirstName())) {
            update.set("firstName", updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null && !updateRequest.getLastName().equals(user.getLastName())) {
            update.set("lastName", updateRequest.getLastName());
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            update.set("email", updateRequest.getEmail());
        }

        if (updateRequest.getPassword() != null && !passwordEncoder.matches(updateRequest.getPassword(), user.getPassword())) {
            update.set("password", passwordEncoder.encode(updateRequest.getPassword()));
        }

        if (!update.getUpdateObject().isEmpty()) {
            System.out.println("THERE IS SOMETHING IN UPDATE");
            UpdateResult updateResult = mongoTemplate.updateFirst(query, update, UserEntity.class);
            if (updateResult.getModifiedCount()>0) return true;            
        } else {
            System.out.println("THERE IS NOTHING TO UPDATE");
        }*/

        updatePropertiesMap.entrySet().stream().forEach(e -> {
            update.set(e.getKey(), e.getValue());
        });

        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, UserEntity.class);

        if (updateResult.getModifiedCount() > 0) return true;

        return false;
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
