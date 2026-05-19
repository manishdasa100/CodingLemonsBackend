package com.codinglemonsbackend.Entities;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
}
