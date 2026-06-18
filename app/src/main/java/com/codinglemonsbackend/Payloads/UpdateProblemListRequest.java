package com.codinglemonsbackend.Payloads;

import com.codinglemonsbackend.Entities.StudyPlanDifficultyTier;
import com.codinglemonsbackend.Validation.CrossFieldValidation;
import com.codinglemonsbackend.Validation.CrossFieldValidation.ValidationType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
public class UpdateProblemListRequest {

    @NotBlank
    private String id;
    
    @Size(min = 5, max = 15)
    private String name;

    @Size(min = 5, max = 50)
    private String description;

    private Boolean isStudyPlan;
    
    private StudyPlanDifficultyTier difficultyTier;

    @PositiveOrZero
    private Integer timelineDays;

    private Boolean isPublic;

    private Boolean isPinned;
}
