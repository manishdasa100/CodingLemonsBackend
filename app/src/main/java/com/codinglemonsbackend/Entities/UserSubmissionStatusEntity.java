package com.codinglemonsbackend.Entities;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "UserSubmissionStatus")
public class UserSubmissionStatusEntity {

    @Id
    private String username;

    @Builder.Default
    private Set<Integer> solvedProblemIds = new HashSet<>();

    @Builder.Default
    private Set<Integer> attemptedProblemIds = new HashSet<>();

    @Builder.Default
    private Map<String, Integer> solvedCountByDifficulty = new HashMap<>(Map.of(
        Difficulty.EASY.name(), 0,
        Difficulty.MEDIUM.name(), 0,
        Difficulty.HARD.name(), 0
    ));

    @Builder.Default
    private Map<String, Integer> solvedCountByLanguage = new HashMap<>();
}
