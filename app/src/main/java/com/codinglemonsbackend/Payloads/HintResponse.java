package com.codinglemonsbackend.Payloads;

import java.time.Instant;

import com.codinglemonsbackend.Dto.ExecutionStatus;

public record HintResponse(
    ExecutionStatus status,
    int level,
    int maxLevel,
    boolean isFinal,
    String content,
    Instant createdAt
) {} 