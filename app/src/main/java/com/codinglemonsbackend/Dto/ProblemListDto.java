package com.codinglemonsbackend.Dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemListDto {
    
    private String id;
    
    private String name;

    private String description;

    private List<ProblemDto> problemsData;

}
