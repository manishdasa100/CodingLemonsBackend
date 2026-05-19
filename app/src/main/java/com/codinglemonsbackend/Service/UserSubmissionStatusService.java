package com.codinglemonsbackend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.UserSubmissionStatusEntity;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Repository.UserSubmissionStatusRepository;

@Service
public class UserSubmissionStatusService {

    @Autowired
    private UserSubmissionStatusRepository userSubmissionStatusRepository;

    @Async("applicationAsyncExecutor")
    @EventListener
    public void createForUser(UserAccountCreationEvent event) {
        String username = event.getUser().getUsername();
        userSubmissionStatusRepository.createForUser(username);
    }

    /**
     * Moves problemId to the solved set and removes it from attempted.
     *
     * @return true if this is a new solve (problem was not previously in solved set)
     */
    public boolean addToSolvedAndRemoveFromAttempted(String username, Integer problemId) {
        return userSubmissionStatusRepository.addToSolvedAndRemoveFromAttempted(username, problemId);
    }

    /**
     * Adds problemId to the attempted set only if it is not already in the solved set.
     */
    public void addToAttemptedIfNotSolved(String username, Integer problemId) {
        userSubmissionStatusRepository.addToAttemptedIfNotSolved(username, problemId);
    }

    public UserSubmissionStatusEntity getSubmissionStatus(String username) {
        return userSubmissionStatusRepository.getByUsername(username);
    }
}
