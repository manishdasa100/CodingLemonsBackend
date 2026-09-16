package com.codinglemonsbackend.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * What every entry on the execution-results stream carries, whoever produced it: the NSJAIL
 * worker writes this shape directly, and the Judge0 poller writes the same shape once it has
 * collected a submission's results. {@code executionReport} is an escaped JSON string in the
 * producing executor's own format - only that executor's parser reads it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExecutionResultEnvelope(
    String jobId,
    String status,
    String workerType,
    String executionReport
) {}
