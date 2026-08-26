package com.codinglemonsbackend.Entities;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProblemExecutionLimits {
    @NotNull
    @Min(value = 1, message = "CPU time limit must be greater than zero")
    @Max(value = 5000, message = "CPU time limit must be less than or equal to 5000 milliseconds")
    private Integer cpuTimeLimit;

    @NotNull    
    @Min(value = 1, message = "Memory limit must be greater than zero")
    @Max(value = 100, message = "Memory limit must be less than or equal to 100 mb")
    private Integer memoryLimit;
    
    @NotNull
    @Min(value = 1, message = "Stack limit must be greater than zero")
    @Max(value = 1024, message = "Stack limit must be less than or equal to 1024 mb")
    private Integer stackLimit;

    @NotNull
    private String optimalTimeComplexity;

    @NotNull
    private String optimalSpaceComplexity;
}
