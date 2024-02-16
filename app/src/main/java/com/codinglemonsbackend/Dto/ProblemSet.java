package com.codinglemonsbackend.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemSet {
    
    private Long total;

    private List<ProblemDto> problems;
}
