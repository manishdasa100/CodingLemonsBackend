package com.codinglemonsbackend.Dto;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubmissionMetadata {

    private String submissionJobId;
    
    private ProblemDto problemDto;

    private ProgrammingLanguage language;

    private String userCode;

    private String username;
}
