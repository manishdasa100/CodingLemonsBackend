package com.codinglemonsbackend.Events;

import org.springframework.context.ApplicationEvent;

import com.codinglemonsbackend.Entities.UserEntity;

public class UserAccountCreationEvent extends ApplicationEvent{

    private UserEntity user;

    // Add time here of account creation to be used later

    public UserAccountCreationEvent(Object source, UserEntity user) {
        super(source);
        this.user = user;
    }

    public UserEntity getUser() {
        return this.user;
    }
    
}
