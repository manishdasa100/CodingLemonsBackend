package com.codinglemonsbackend.Dto;

public enum BadgeRuleType {
    STREAK_DAYS("Streak");

    private final String displayName;

    BadgeRuleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
