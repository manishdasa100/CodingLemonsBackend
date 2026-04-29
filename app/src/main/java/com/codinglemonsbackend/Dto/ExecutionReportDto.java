package com.codinglemonsbackend.Dto;

import java.util.List;

import lombok.Builder;

@Builder
public record ExecutionReportDto(
    String executionId,
    String language,
    String task,
    Integer totalTestcases,
    Integer totalCorrect,
    ExecutionStatus status,
    String statusMsg,
    int runtimeMs,
    int memoryMb,
    List<TestcaseResult> testcaseResults,
    TestcaseResult failedTestcase,
    String compileError,
    String runtimeError
) {}
