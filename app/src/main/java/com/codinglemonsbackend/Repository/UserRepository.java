package com.codinglemonsbackend.Repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Payloads.UserUpdateRequestPayload;

@Repository
public class UserRepository {

    private MongoTemplate mongoTemplate;

    public UserRepository(@Autowired MongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<UserEntity> getUser(String username){
        System.out.println("Username: "+ username);
        UserEntity user = mongoTemplate.findById(username, UserEntity.class, "Users");
        return Optional.ofNullable(user);
    }

    public void saveUser(UserEntity user){
        mongoTemplate.save(user, "Users");
    }

    public boolean updateUserInfo(UserEntity currentlySignedInUser, UserUpdateRequestPayload updateRequest) {
        
        Query query = new Query(Criteria.where("username").is(currentlySignedInUser.getUsername()));

        Update update = new Update();
        
        if (updateRequest.getFirstName() != null && !updateRequest.getFirstName().equals(currentlySignedInUser.getFirstName())) {
            update.set("firstName", updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null && !updateRequest.getLastName().equals(currentlySignedInUser.getLastName())) {
            update.set("lastName", updateRequest.getLastName());
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(currentlySignedInUser.getEmail())) {
            update.set("email", updateRequest.getEmail());
        }

        if (!update.getUpdateObject().isEmpty()) {
            System.out.println("THERE IS SOMETHING IN UPDATE");
            mongoTemplate.updateFirst(query, update, UserEntity.class);
            return true;
        } else {
            System.out.println("THERE IS NOTHING TO UPDATE");
        }

        return false;
    }

    public void updateUserProfilePictureId(String username, String profilePictureId) {

        Query query = new Query(Criteria.where("username").is(username));

        Update update = new Update();

        if (profilePictureId != null) { 
            update.set("profilePictureId", profilePictureId);
        } else {
            throw new IllegalArgumentException("profilePictureId cannot be null");
        }

        if (!update.getUpdateObject().isEmpty()) {
            System.out.println("UPDATING PROFILE PICTURE ID");
            mongoTemplate.updateFirst(query, update, UserEntity.class);
        }
    }
    
}
