package com.codinglemonsbackend.Dto;

import java.util.List;

import com.codinglemonsbackend.Entities.StudyPlanDifficultyTier;
import com.codinglemonsbackend.Entities.UserStudyPlanProgress;
import com.codinglemonsbackend.Validation.CrossFieldValidation;
import com.codinglemonsbackend.Validation.CrossFieldValidation.ValidationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
//@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_NULL)
@CrossFieldValidation(rules = {
    @CrossFieldValidation.FieldRule(
        field = "difficultyTier",
        dependsOn = "isStudyPlan",
        type = ValidationType.REQUIRED_IF_VALUES,
        values = {"true"},
        message = "difficultyTier is required when isStudyPlan is true"
    ),
    @CrossFieldValidation.FieldRule(
        field = "timelineDays",
        dependsOn = "isStudyPlan",
        type = ValidationType.REQUIRED_IF_VALUES,
        values = {"true"},
        message = "timelineDays is required when isStudyPlan is true"
    )
})
public class ProblemListDto {
    
    @JsonProperty(access = Access.READ_ONLY)
    private String id;
    
    @NotEmpty
    @Size(max = 30)
    private String name;

    @Size(max = 100)
    private String description;

    @JsonProperty(access = Access.READ_ONLY)
    private List<ProblemDto> problemsData;

    @JsonProperty(access = Access.READ_ONLY)
    private Integer totalProblems;

    private Boolean isStudyPlan = false;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean isActiveIfStudyPlan;

    @JsonProperty(access = Access.READ_ONLY)
    private UserStudyPlanProgress studyPlanProgress;
    
    private StudyPlanDifficultyTier difficultyTier;

    @PositiveOrZero
    private Integer timelineDays;

    @JsonProperty(access = Access.READ_ONLY)
    private List<Integer> solvedProblemIds;

    @JsonProperty(access = Access.READ_ONLY)
    private String creator;

    private Boolean isPublic = false;
    
    private Boolean isPinned = false;
}
