package com.codinglemonsbackend.Repository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.codinglemonsbackend.Entities.UserEntity;

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
    
}
