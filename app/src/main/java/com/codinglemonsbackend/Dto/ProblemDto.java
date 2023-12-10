package com.codinglemonsbackend.Dto;

import java.util.List;
import java.util.Map;

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
@JsonIgnoreProperties(value={"testCases", "testCaseOutputs", "driverCodes", "optimalSolutions"}, allowSetters = true)
@JsonDeserialize(using=ProblemEntityDeserializer.class) 
public class ProblemDto {
    
    @NotEmpty
    @Size(min = 5, max = 30)
    String title;

    @NotEmpty
    @Size(min = 20, max = 100)
    String description;

    @NotEmpty
    List<String> constraints;

    @NotEmpty
    List<Example> examples;

    @NotEmpty
    List<String> testCases;

    @NotEmpty
    List<String> testCaseOutputs;

    @NotNull(message = "Difficulty must be either EASY/MEDIUM/HARD")
    Difficulty difficulty;

    @NotEmpty
    Map<ProgrammingLanguage, String> driverCodes;

    @NotEmpty
    Map<ProgrammingLanguage, String> optimalSolutions;
    
    @NotEmpty
    List<String> topics;

    @JsonProperty(access = Access.READ_ONLY)
    Integer acceptance;
}
