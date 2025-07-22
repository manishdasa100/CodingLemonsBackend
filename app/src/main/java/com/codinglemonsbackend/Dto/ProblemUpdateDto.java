package com.codinglemonsbackend.Dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Updates cannot be null")
    private Map<String, Object> updates;
    
    // private String title;

    // private String description;

    // private Set<String> constraints;

    // private Set<Example> examples;

    // private Map<ProgrammingLanguage, String> codeSnippets;

    // private Difficulty difficulty;

    // private Float cpuTimeLimit;

    // private Float memoryLimit;

    // private Integer stackLimit;

    // private Set<String> topicSlugs;

    // private Set<String> companySlugs;

    // @JsonIgnore
    // private Integer previousProblemId;

    // @JsonIgnore
    // private Integer nextProblemId;
}
