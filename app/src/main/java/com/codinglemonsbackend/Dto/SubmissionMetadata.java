package com.codinglemonsbackend.Dto;

import java.time.ZoneId;

import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;
import com.codinglemonsbackend.Entities.ProblemExecutionLimits;
import com.codinglemonsbackend.Payloads.SubmissionType;

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

    private ProblemExecutionLimits executionLimits;

    private ProgrammingLanguage language;

    private String userCode;

    private String username;

    private SubmissionType submissionType;

    private Boolean b64Encoded;

    private Difficulty difficulty;

    private String listId;

    private ZoneId resolvedZoneId;
}
