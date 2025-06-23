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
    private float cpuTimeLimit;
    private float memoryLimit;
    private Integer stackLimit;
}
