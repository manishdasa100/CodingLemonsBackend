package com.codinglemonsbackend.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.codinglemonsbackend.Entities.UserStreakEntity;
import com.codinglemonsbackend.Events.StreakUpdatedEvent;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Repository.UserStreakRepositoryService;
import com.codinglemonsbackend.Utils.ZoneUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserStreakService {

    private final UserStreakRepositoryService repositoryService;

    private final ApplicationEventPublisher eventPublisher;

    private final ZoneUtils zoneUtils;

    public UserStreakEntity getStreak(String username, String zoneId) {
        UserStreakEntity streak = repositoryService.getUserStreak(username)
                .orElseThrow(() -> new NoSuchElementException("User streak not found for user: " + username));
        
        LocalDate today = LocalDate.now(zoneUtils.resolveZone(username, zoneId));
        LocalDate last = streak.getLastSubmissionDate();

        boolean streakBroken = last == null || last.isBefore(today.minusDays(1));
        if (streakBroken && streak.getStreakDays() != 0) {
            streak.setStreakDays(0);
            repositoryService.saveUserStreak(streak);
        }

        return streak;
    }

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onSubmitCodeCompleted(SubmitCodeCompletedEvent event) {
        String username = event.getSubmissionMetadata().getUsername();
        
        UserStreakEntity streak = repositoryService.getUserStreak(username)
        .orElseThrow(() -> new NoSuchElementException("User streak not found for user: " + username));
        
        LocalDate today = LocalDate.now(event.getSubmissionMetadata().getResolvedZoneId());
        if (today.equals(streak.getLastSubmissionDate())) return;

        LocalDate yesterday = today.minusDays(1);
        int newStreakDays = yesterday.equals(streak.getLastSubmissionDate()) ? streak.getStreakDays() + 1: 1;

        streak.setStreakDays(newStreakDays);
        streak.setLastSubmissionDate(today);

        if (newStreakDays > streak.getHighestStreakDays()) {
            streak.setHighestStreakDays(newStreakDays);
            streak.setHighestStreakDate(today);
        }

        repositoryService.saveUserStreak(streak);
        eventPublisher.publishEvent(new StreakUpdatedEvent(this, username, newStreakDays));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createUserStreak(String username) {
        UserStreakEntity entity = new UserStreakEntity(
            username,
            0,
            null,
            0,
            null
        );
        repositoryService.saveUserStreak(entity);
    }
    
}
