package com.codinglemonsbackend.Payloads;

import java.util.List;

import com.codinglemonsbackend.Entities.StatusMessage;
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
@JsonInclude(Include.NON_NULL)
public class CodeSubmissionResponse {

    private Boolean runSuccess;
    private String error;
    private List<String> codeAnswer;
    private List<String> expectedAnswer;
    private Integer totalTestCases;
    private Integer totalCorrect;
    private String failedTestCase;
    private StatusMessage statusMsg;
    
}
