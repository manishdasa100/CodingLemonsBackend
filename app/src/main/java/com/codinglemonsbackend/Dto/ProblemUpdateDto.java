package com.codinglemonsbackend.Dto;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemUpdateDto {
    
    private String title;

    private String description;

    private Set<String> constraints;

    private Set<Example> examples;

    private Map<ProgrammingLanguage, String> codeSnippets;

    private Difficulty difficulty;

    private Float cpuTimeLimit;

    private Float memoryLimit;

    private Integer stackLimit;

    private Set<String> topicSlugs;

    private Set<String> companySlugs;

    @JsonIgnore
    private Integer previousProblemId;

    @JsonIgnore
    private Integer nextProblemId;
}
