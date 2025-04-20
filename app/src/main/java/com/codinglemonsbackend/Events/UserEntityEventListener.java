package com.codinglemonsbackend.Events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Entities.UserEntity;
import com.codinglemonsbackend.Service.UserProblemListRepositoryService;

@Component
public class UserEntityEventListener extends AbstractMongoEventListener<UserEntity>{

    @Autowired
    private UserProblemListRepositoryService userProblemListRepositoryService;

    @Override
    public void onAfterSave(AfterSaveEvent<UserEntity> event){
        
        System.out.println("User saved");

        String username = event.getSource().getUsername();

        userProblemListRepositoryService.createDefaultProblemList(username);
    }
    
}
