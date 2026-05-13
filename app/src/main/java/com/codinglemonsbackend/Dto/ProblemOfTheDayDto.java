package com.codinglemonsbackend.Dto;

import java.util.Set;

import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;

public record ProblemOfTheDayDto(
    Integer problemId,
    String title,
    Difficulty difficulty,
    Set<String> topics
) {}