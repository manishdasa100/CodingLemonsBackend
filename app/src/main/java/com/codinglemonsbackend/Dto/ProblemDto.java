package com.codinglemonsbackend.Dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codinglemonsbackend.Entities.ProblemExecutionLimits;
import com.codinglemonsbackend.Utils.ProblemDtoDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
//@JsonIgnoreProperties(value={"testCases", "testCaseOutputs", "driverCodes", "cpuTimeLimit", "memoryLimit", "stackLimit"}, allowSetters = true)
@JsonDeserialize(using=ProblemDtoDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_NULL)
public class ProblemDto implements Serializable {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(value = Include.NON_NULL)
    public static class Example implements Serializable {
        private String input;
        private String output;
        private String explanation;
    }

    public enum Difficulty {
        EASY(1),
        MEDIUM(2),
        HARD(3);

        private Integer points;

        private Difficulty(Integer points) {
            this.points = points;
        }

        public Integer getPoints() {
            return points;
        }
    }

    private Integer id;

    @NotEmpty
    @Size(min = 5, max = 60)
    private String title;

    @NotEmpty
    @Size(min = 20, max = 500)
    private String description;

    @NotEmpty
    private List<String> constraints;

    @NotNull(message = "Difficulty must be either EASY/MEDIUM/HARD")
    private Difficulty difficulty;

    @NotEmpty
    private List<Example> examples;

    @NotEmpty
    private Map<SupportedLanguage, String> codeSnippets;

    @JsonIgnore
    private ProblemExecutionLimits executionLimits;
    
    @NotEmpty
    private Set<String> topics;

    private Set<String> companies;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer likes;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer previousProblemId;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer nextProblemId;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer acceptedCount;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer submissionCount;

    @JsonProperty(access = Access.READ_ONLY)
    private ProblemStatus status;

    @JsonProperty(access = Access.READ_ONLY)
    private UserSubmissionStatus userSubmissionStatus;
}
