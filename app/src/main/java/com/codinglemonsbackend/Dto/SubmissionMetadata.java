package com.codinglemonsbackend.Dto;

import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;

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
    
    private Integer problemId;

    private Integer solutionPoints;

    private ProblemExecutionDetails executionDetails;

    private ProgrammingLanguage language;

    private String userCode;

    private String username;

    private Boolean isRunCode;

    private Boolean b64Encoded;

    private Difficulty difficulty;
}
