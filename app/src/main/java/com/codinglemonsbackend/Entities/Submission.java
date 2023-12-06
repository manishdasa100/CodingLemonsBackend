package com.codinglemonsbackend.Entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Submission {
    private String submissionId;
    private Integer problemId;
    private String language;
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
