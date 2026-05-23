package com.codinglemonsbackend.Dto;

public enum BadgeRuleType {
    STREAK_DAYS("Streak"),
    PROBLEMS_SOLVED("Problems Solved");

    private final String displayName;

    BadgeRuleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
