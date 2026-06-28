package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Dto.TestcaseResult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Submissions")
@CompoundIndex(def = "{'username':1, 'problemId':1}")
public class SubmissionEntity {
    @Id
    private String submissionId;
    private String username;
    private Integer problemId;
    private ProgrammingLanguage language;
    private String userCode;
    private int runtimeMs;
    private int memoryMb;
    private String dateOfSubmission;
    private Boolean runSucccess;
    private Integer totalTestCases;
    private Integer totalCorrectOutput;
    private TestcaseResult failedTestCase;
    private ExecutionStatus status;
}
