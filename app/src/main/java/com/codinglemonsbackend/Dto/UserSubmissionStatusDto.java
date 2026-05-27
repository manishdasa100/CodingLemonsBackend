package com.codinglemonsbackend.Dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSubmissionStatusDto {
    private String username;
    private Map<String, Integer> solvedCountByDifficulty;
}
