package com.codinglemonsbackend.Dto;

import lombok.Builder;

@Builder
public record TestcaseResult(
    Integer index,
    TestcaseStatus status,
    String input,
    String expectedOutput,
    String actualOutput,
    String stdOut,
    String stdErr,
    Integer runtimeMs,
    Integer memoryMb,
    String errorMsg
) {}
