package com.codinglemonsbackend.Dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codinglemonsbackend.Entities.Difficulty;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.codinglemonsbackend.Utils.ProblemEntityDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
//@JsonIgnoreProperties(value={"testCases", "testCaseOutputs", "driverCodes", "cpuTimeLimit", "memoryLimit", "stackLimit"}, allowSetters = true)
@JsonDeserialize(using=ProblemEntityDeserializer.class) 
public class ProblemDto implements Serializable {

    @JsonProperty(access = Access.READ_ONLY)
    Integer problemId;
    
    @NotEmpty
    @Size(min = 5, max = 30)
    String title;

    @NotEmpty
    @Size(min = 20, max = 500)
    String description;

    @NotEmpty
    Set<String> constraints;

    @NotEmpty
    Set<Example> examples;

    @NotEmpty
    @JsonProperty(access = Access.WRITE_ONLY)
    Map<String, String> testCasesWithExpectedOutputs;

    @NotEmpty
    @JsonProperty(access = Access.WRITE_ONLY)
    List<String> testCaseOutputs;

    // @NotEmpty
    // Map<String, String> testcaseAndExpectedOutputs;

    @NotNull(message = "Difficulty must be either EASY/MEDIUM/HARD")
    Difficulty difficulty;

    @NotEmpty
    @JsonProperty(access = Access.WRITE_ONLY)
    Map<ProgrammingLanguage, String> driverCodes;

    @NotNull
    @JsonProperty(access = Access.WRITE_ONLY)
    Float cpuTimeLimit;

    @NotNull
    @JsonProperty(access = Access.WRITE_ONLY)
    Float memoryLimit;

    @NotNull
    @JsonProperty(access = Access.WRITE_ONLY)
    Integer stackLimit;

    // @NotEmpty
    // Map<ProgrammingLanguage, String> optimalSolutions;
    
    @NotEmpty
    Set<String> topics;

    Set<String> companyTags;

    @JsonProperty(access = Access.READ_ONLY)
    Integer acceptance;

    @JsonProperty(access = Access.READ_ONLY)
    Integer previousProblemId;

    @JsonProperty(access = Access.READ_ONLY)
    Integer nextProblemId;
}
