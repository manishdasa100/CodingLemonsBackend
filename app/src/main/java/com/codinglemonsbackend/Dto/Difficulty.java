package com.codinglemonsbackend.Dto;

public enum Difficulty {
    EASY(1),
    MEDIUM(2),
    HARD(3);

    private Integer points;

    private Difficulty(Integer points) {
        this.points = points;
    }

    public Integer getPoints() {
        return points;
    }
}
    

