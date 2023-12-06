package com.codinglemonsbackend.Repository;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.codinglemonsbackend.Entities.Role;
import com.codinglemonsbackend.Entities.UserEntity;


@DataMongoTest
@TestInstance(Lifecycle.PER_CLASS)
public class UserRepositoryTest {
    
    /* Start the local mongoDB server before testing */  
    
    @Autowired
    private MongoTemplate mongoTemplate;

    private UserRepository underTest;

    @BeforeAll
    void setUp() {
        underTest = new UserRepository(mongoTemplate);
    }

    @AfterEach
    void tearDown(){
        mongoTemplate.dropCollection("Users");
    }

    @Test
    public void testGetUser_success() {

        //given
        UserEntity entityToSave = UserEntity.builder()
                                    .username("KingKohli")
                                    .email("email@example.com")
                                    .firstName("Virat")
                                    .lastName("Kohli")
                                    .password("password")
                                    .role(Role.USER)
                                    .submissions(new ArrayList<>())
                                    .build();

        mongoTemplate.save(entityToSave, "Users");

        //when
        Optional<UserEntity> user = underTest.getUser("KingKohli");

        //then
        assertThat(user.isPresent()).isTrue();

        assertThat(user.get()).isEqualTo(entityToSave);

    }

    @Test
    public void testGetUser_userNotPresent() {

        //when
        Optional<UserEntity> user = underTest.getUser("KingKohli");

        //then
        assertThat(user.isPresent()).isFalse();

    }

    @Test
    public void testSaveUser() {

        String username = "KingKohli";
        
        UserEntity entityToSave = UserEntity.builder()
                                    .username(username)
                                    .email("email@example.com")
                                    .firstName("Virat")
                                    .lastName("Kohli")
                                    .password("password")
                                    .role(Role.USER)
                                    .submissions(new ArrayList<>())
                                    .build();
        
        underTest.saveUser(entityToSave);

        UserEntity user = mongoTemplate.findOne(new Query(Criteria.where("username").is(username)), UserEntity.class);

        assertThat(user).isEqualTo(entityToSave);
    }       
}
