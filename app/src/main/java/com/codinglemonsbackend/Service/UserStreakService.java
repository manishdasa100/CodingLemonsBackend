package com.codinglemonsbackend.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.UserStreakEntity;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Repository.UserStreakRepositoryService;

@Service
public class UserStreakService {

    @Autowired
    private UserStreakRepositoryService repositoryService;

    public UserStreakEntity getStreak(String username) {
        Optional<UserStreakEntity> optional = repositoryService.getUserStreak(username);
        try {
            UserStreakEntity userStreakEntity = optional.get();
            return userStreakEntity;
        } catch(NoSuchElementException e) {
            throw new NoSuchElementException("User streak entity not found");
        }
    }

    @Async
    @EventListener
    public void createUserStreak(UserAccountCreationEvent event) {
        UserStreakEntity entity = new UserStreakEntity(
            event.getUser().getUsername(),
            0,
            null
        );
        repositoryService.saveUserStreak(entity);
    }
    
}
