package com.codinglemonsbackend.Events;

import org.springframework.context.ApplicationEvent;

public class ProblemRegistryUpdatedEvent extends ApplicationEvent{

    // Some kind of id to identify this event
    // For example, serialID

    private final Integer problemId;

    public ProblemRegistryUpdatedEvent(Object source, Integer problemId) {
        super(source);
        this.problemId = problemId;
    }

    public Integer getProblemId() {
        return problemId;
    }
    
}
