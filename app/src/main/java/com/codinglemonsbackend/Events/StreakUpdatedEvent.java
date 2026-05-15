package com.codinglemonsbackend.Events;

import org.springframework.context.ApplicationEvent;

public class StreakUpdatedEvent extends ApplicationEvent {

    private final String username;
    private final int newStreakDays;

    public StreakUpdatedEvent(Object source, String username, int newStreakDays) {
        super(source);
        this.username = username;
        this.newStreakDays = newStreakDays;
    }

    public String getUsername() {
        return username;
    }

    public int getNewStreakDays() {
        return newStreakDays;
    }
}
