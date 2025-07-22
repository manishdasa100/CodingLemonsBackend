package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProblemExecutionDetails {
    
    private Integer id;
    private Float cpuTimeLimit;
    private Float memoryLimit;
    private Integer stackLimit;
}
