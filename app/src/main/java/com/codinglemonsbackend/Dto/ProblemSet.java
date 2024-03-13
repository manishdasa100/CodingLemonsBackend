package com.codinglemonsbackend.Dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemSet implements Serializable{
    
    private Long total;

    private List<ProblemDto> problems;
}
