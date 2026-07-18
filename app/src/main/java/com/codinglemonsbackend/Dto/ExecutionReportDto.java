package com.codinglemonsbackend.Dto;

import java.util.List;
import java.util.Map;

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
    Integer runtimeMs,
    Integer memoryMb,
    List<TestcaseResult> testcaseResults,
    TestcaseResult failedTestcase,
    String compileError,
    String runtimeError,
    String internalError,
    Map<String, Object> calibrationReport
) {}
