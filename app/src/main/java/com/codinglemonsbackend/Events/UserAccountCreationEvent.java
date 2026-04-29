package com.codinglemonsbackend.Events;

import org.springframework.context.ApplicationEvent;

import com.codinglemonsbackend.Dto.UserDto;

public class UserAccountCreationEvent extends ApplicationEvent{

    // Some kind of id to identify this event
    // For example, serialID

    private UserDto user;

    // Add time here of account creation to be used later

    public UserAccountCreationEvent(Object source, UserDto user) {
        super(source);
        this.user = user;
    }

    public UserDto getUser() {
        return this.user;
    }
    
}
