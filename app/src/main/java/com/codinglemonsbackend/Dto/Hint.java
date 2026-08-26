package com.codinglemonsbackend.Dto;

import java.time.Instant;

public record Hint(
    int level,
    String content,
    String submissionId,
    String provider,
    Instant createdAt
) {}
