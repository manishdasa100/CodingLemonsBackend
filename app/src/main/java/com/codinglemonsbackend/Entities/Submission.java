package com.codinglemonsbackend.Entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Submissions")
public class Submission {
    @Id
    private String submissionId;
    private List<String> submissionTokens;
    private String username;
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
