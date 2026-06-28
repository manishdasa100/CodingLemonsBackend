package com.codinglemonsbackend.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(value = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubmissionDto {
    
    private String submissionId;
    private String username;
    private Integer problemId;
    private ProblemDto problemData;
    private ProgrammingLanguage language;
    private String userCode;
    private String dateOfSubmission;
    private Boolean runSucccess;
    private Integer runtimeMs;
    private Integer memoryMb;
    private String error;
    private Integer totalTestCases;
    private Integer totalCorrectOutput;
    private TestcaseResult failedTestCase;
    private ExecutionStatus status;
}
