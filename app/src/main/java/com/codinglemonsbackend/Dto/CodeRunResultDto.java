package com.codinglemonsbackend.Dto;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.codinglemonsbackend.Entities.StatusMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(value = Include.NON_NULL)
public class CodeRunResultDto {
    
    private String submissionId;
    private ProgrammingLanguage language;
    private float runtime;
    private Boolean runSuccess;
    private String error;
    private Integer totalTestCases;
    private Integer totalCorrectOutput;
    private StatusMessage statusMessage;
    private List<String> testCases;
    private List<String> codeAnswer;
    private List<String> expectedCodeAnswer;
}
