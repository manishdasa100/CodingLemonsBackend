package com.codinglemonsbackend.Dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemUpdateDto {
    
    private String title;

    private String description;

    private Set<String> constraints;

    private Set<Example> examples;

    private Map<String, String> testCasesWithExpectedOutputs;

    private List<String> testCaseOutputs;

    private Map<ProgrammingLanguage, String> driverCodes;

    private Difficulty difficulty;

    private Float cpuTimeLimit;

    private Float memoryLimit;

    private Integer stackLimit;

    private Set<String> topics;

    private Set<String> companyTags;

    @JsonIgnore
    private Integer previousProblemId;

    @JsonIgnore
    private Integer nextProblemId;
}
