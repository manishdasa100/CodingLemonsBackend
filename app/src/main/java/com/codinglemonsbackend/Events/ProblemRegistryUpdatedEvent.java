package com.codinglemonsbackend.Events;

import org.springframework.context.ApplicationEvent;

public class ProblemRegistryUpdatedEvent extends ApplicationEvent{

    private final Integer problemId;

    public ProblemRegistryUpdatedEvent(Object source, Integer problemId) {
        super(source);
        this.problemId = problemId;
    }

    public Integer getProblemId() {
        return problemId;
    }
    
}
