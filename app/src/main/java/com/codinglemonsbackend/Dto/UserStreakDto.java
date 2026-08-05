package com.codinglemonsbackend.Dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserStreakDto {
    private String username;
    private Integer streakDays;
    private LocalDate lastSubmissionDate;
    private Integer highestStreakDays;
    private LocalDate highestStreakDate; 
    private BadgeDto highestStreakBadge;
    private Integer nextBadgeThreshold;
}
