package com.codinglemonsbackend.Payloads;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.codinglemonsbackend.Dto.Difficulty;
import com.codinglemonsbackend.Dto.Example;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Entities.Company;
import com.codinglemonsbackend.Entities.Topic;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_NULL)
public class CreateProblemRequestPayload {
    
    @NotEmpty
    @Size(min = 5, max = 60)
    private String title;

    @NotEmpty
    @Size(min = 20, max = 500)
    private String description;

    @NotEmpty
    private Set<String> constraints;

    @NotEmpty
    private Set<Example> examples;

    @NotNull(message = "Difficulty must be either EASY/MEDIUM/HARD")
    private Difficulty difficulty;

    // @NotEmpty
    // @JsonProperty(access = Access.WRITE_ONLY)
    // private LinkedHashMap<String, String> testCasesWithExpectedOutputs;

    // @NotEmpty
    // private Map<ProgrammingLanguage, String> codeSnippets;

    // @NotEmpty
    // @JsonProperty(access = Access.WRITE_ONLY)
    // private Map<ProgrammingLanguage, String> driverCodes;

    @NotNull
    @JsonProperty(access = Access.WRITE_ONLY)
    private Float cpuTimeLimit;

    @NotNull
    @JsonProperty(access = Access.WRITE_ONLY)
    private Float memoryLimit;

    @NotNull
    @JsonProperty(access = Access.WRITE_ONLY)
    private Integer stackLimit;
    
    @NotEmpty
    private Set<String> topics;

    private Set<String> companies;

}
