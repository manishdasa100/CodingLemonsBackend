package com.codinglemonsbackend.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.BadgeRuleType;
import com.codinglemonsbackend.Entities.BadgeEntity;
import com.codinglemonsbackend.Entities.UserProfileEntity;
import com.codinglemonsbackend.Events.StreakUpdatedEvent;
import com.codinglemonsbackend.Repository.BadgeRepository;
import com.codinglemonsbackend.Repository.UserProfileRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BadgeEvaluatorService {

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Async("applicationAsyncExecutor")
    @EventListener
    public void onStreakUpdated(StreakUpdatedEvent event) {
        String username = event.getUsername();
        int streakDays = event.getNewStreakDays();

        UserProfileEntity profile = userProfileRepository.getUserProfile(username).orElse(null);
        if (profile == null) {
            log.warn("Badge evaluation skipped: profile not found for user {}", username);
            return;
        }

        Set<String> alreadyEarned = profile.getEarnedBadgeIds() != null
                ? Set.copyOf(profile.getEarnedBadgeIds())
                : Set.of();

        List<BadgeEntity> qualifyingBadges = badgeRepository.findByRuleType(BadgeRuleType.STREAK_DAYS)
                .stream()
                .filter(b -> !alreadyEarned.contains(b.getId()))
                .filter(b -> streakDays >= b.getRule().getThreshold())
                .collect(Collectors.toList());

        if (qualifyingBadges.isEmpty()) return;

        for (BadgeEntity badge : qualifyingBadges) {
            userProfileRepository.addEarnedBadge(username, badge.getId());
            log.info("Awarded badge '{}' to user {}", badge.getName(), username);
        }
    }
}
