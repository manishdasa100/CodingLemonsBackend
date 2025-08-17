package com.codinglemonsbackend.Events;

import org.springframework.context.ApplicationEvent;

import com.codinglemonsbackend.Dto.UserProfileDto;

public class UserProfileUpdateEvent extends ApplicationEvent {
    
    // Some kind of id to identify this event
    // For example, serialID
    private String username;
    private UserProfileDto newUserProfileDetails;

    public UserProfileUpdateEvent(Object source, String username, UserProfileDto userProfileDetails) {
        super(source);
        this.username = username;
        this.newUserProfileDetails = userProfileDetails;
    }

    public String getUsername() {
        return username;
    }

    public UserProfileDto getNewUserProfileDetails() {
        return newUserProfileDetails;
    }
}
