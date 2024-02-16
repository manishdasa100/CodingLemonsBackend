package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemDtoWithStatus {
    
    private ProblemStatus status;

    private ProblemDto problem;
}
