package com.codinglemonsbackend.Dto;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.codinglemonsbackend.Entities.StatusMessage;
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
    
    private Integer problemId;
    private ProgrammingLanguage language;
    private String userCode;
    private Integer runtime;
    private String dateOfSubmission;
    private Boolean runSucccess;
    private String error;
    private Integer totalTestCases;
    private Integer totalCorrectOutput;
    private String failedTestCase;
    private StatusMessage statusMessage;
}
