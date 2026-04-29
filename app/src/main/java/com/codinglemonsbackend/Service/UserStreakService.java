package com.codinglemonsbackend.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Entities.UserStreakEntity;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Events.UserAccountCreationEvent;
import com.codinglemonsbackend.Repository.UserStreakRepositoryService;

@Service
public class UserStreakService {

    @Autowired
    private UserStreakRepositoryService repositoryService;

    public UserStreakEntity getStreak(String username) {
        UserStreakEntity streak = repositoryService.getUserStreak(username)
                .orElseThrow(() -> new NoSuchElementException("User streak not found for user: " + username));
        return streak;
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onSubmitCodeCompleted(SubmitCodeCompletedEvent event) {
        String username = event.getSubmissionMetadata().getUsername();
        
        UserStreakEntity streak = repositoryService.getUserStreak(username)
        .orElseThrow(() -> new NoSuchElementException("User streak not found for user: " + username));
        
        LocalDate today = LocalDate.now();
        if (today.equals(streak.getLastSubmissionDate())) return;

        LocalDate yesterday = LocalDate.now().minusDays(1);
        int newStreakDays = yesterday.equals(streak.getLastSubmissionDate()) ? streak.getStreakDays() + 1: 1;

        streak.setStreakDays(newStreakDays);
        streak.setLastSubmissionDate(today);

        if (newStreakDays > streak.getHighestStreakDays()) {
            streak.setHighestStreakDays(newStreakDays);
            streak.setHighestStreakDate(today);
        }

        repositoryService.saveUserStreak(streak);
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void createUserStreak(UserAccountCreationEvent event) {
        UserStreakEntity entity = new UserStreakEntity(
            event.getUser().getUsername(),
            0,
            null,
            0,
            null
        );
        repositoryService.saveUserStreak(entity);
    }
    
}
