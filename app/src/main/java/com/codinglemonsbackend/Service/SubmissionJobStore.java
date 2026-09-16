package com.codinglemonsbackend.Service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * The per-submission Redis hash that the whole pipeline reads and writes: intake creates it,
 * the executor adapters annotate it, the result processor completes it, and the poll endpoint
 * reads it. Keeping the field names in one place is the point - four collaborators previously
 * would have had to agree on them by hand.
 */
@Slf4j
@Component
public class SubmissionJobStore {

    private static final String KEY_PREFIX = "submission:report:";

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_METADATA = "submissionMetadata";
    private static final String FIELD_WORKER_TYPE = "workerType";
    private static final String FIELD_QUEUED_AT = "queuedAt";
    private static final String FIELD_REPORT = "executionReport";
    private static final String FIELD_CONSUMED = "consumed";
    private static final String FIELD_FAILURE_REASON = "failureReason";
    private static final String FIELD_EXECUTOR_REF = "executorRef";

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final long ttlSeconds;

    public SubmissionJobStore(
            RedisService redisService,
            ObjectMapper objectMapper,
            @Value("${executor.job.ttl-seconds:1800}") long ttlSeconds) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
    }

    public String key(String jobId) {
        return KEY_PREFIX + jobId;
    }

    public boolean exists(String jobId) {
        return Boolean.TRUE.equals(redisService.keyExist(key(jobId)));
    }

    public void delete(String jobId) {
        redisService.deleteKey(key(jobId));
    }

    /** Creates the job in QUEUED state. Must happen before dispatch so a fast result never races the write. */
    public void create(String jobId, SubmissionMetadata metadata, ExecutorWorkerType workerType) {
        String key = key(jobId);
        try {
            redisService.storeHash(key, FIELD_METADATA, objectMapper.writeValueAsString(metadata), ttlSeconds);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize submission metadata for job " + jobId, e);
        }
        redisService.storeHash(key, FIELD_STATUS, PendingOrdersStatus.QUEUED.name(), ttlSeconds);
        redisService.storeHash(key, FIELD_WORKER_TYPE, workerType.name(), ttlSeconds);
        redisService.storeHash(key, FIELD_QUEUED_AT, Long.toString(Instant.now().getEpochSecond()), ttlSeconds);
    }

    public void setWorkerType(String jobId, ExecutorWorkerType workerType) {
        redisService.storeHash(key(jobId), FIELD_WORKER_TYPE, workerType.name(), ttlSeconds);
    }

    public ExecutorWorkerType getWorkerType(String jobId) {
        String value = redisService.getHashValue(key(jobId), FIELD_WORKER_TYPE);
        return value == null ? null : ExecutorWorkerType.valueOf(value);
    }

    public PendingOrdersStatus getStatus(String jobId) {
        String value = redisService.getHashValue(key(jobId), FIELD_STATUS);
        return value == null ? null : PendingOrdersStatus.valueOf(value);
    }

    public String getReportJson(String jobId) {
        return redisService.getHashValue(key(jobId), FIELD_REPORT);
    }

    public String getFailureReason(String jobId) {
        return redisService.getHashValue(key(jobId), FIELD_FAILURE_REASON);
    }

    public SubmissionMetadata getMetadata(String jobId) {
        String json = redisService.getHashValue(key(jobId), FIELD_METADATA);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, SubmissionMetadata.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read submission metadata for job " + jobId, e);
        }
    }

    /** Seconds since the job was queued, or -1 when unknown. */
    public long ageSeconds(String jobId) {
        String queuedAt = redisService.getHashValue(key(jobId), FIELD_QUEUED_AT);
        if (queuedAt == null) return -1;
        try {
            return Instant.now().getEpochSecond() - Long.parseLong(queuedAt);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Executor-specific handle for an in-flight job - Judge0 stores its submission tokens here.
     * The NSJAIL worker needs none: it echoes our job id back.
     */
    public void setExecutorRef(String jobId, String ref) {
        redisService.storeHash(key(jobId), FIELD_EXECUTOR_REF, ref, ttlSeconds);
    }

    public String getExecutorRef(String jobId) {
        return redisService.getHashValue(key(jobId), FIELD_EXECUTOR_REF);
    }

    /**
     * Atomically claims this job for post-processing. Only the caller that gets {@code true}
     * may persist the submission and award points.
     */
    public boolean claimForProcessing(String jobId) {
        return Boolean.TRUE.equals(redisService.putHashIfAbsent(key(jobId), FIELD_CONSUMED, "1"));
    }

    /** Releases a claim so a redelivered result can be retried after a failed attempt. */
    public void releaseClaim(String jobId) {
        redisService.deleteHashEntry(key(jobId), FIELD_CONSUMED);
    }

    public void markCompleted(String jobId, String normalizedReportJson) {
        String key = key(jobId);
        redisService.storeHash(key, FIELD_REPORT, normalizedReportJson, ttlSeconds);
        redisService.storeHash(key, FIELD_STATUS, PendingOrdersStatus.COMPLETED.name(), ttlSeconds);
    }

    public void markFailed(String jobId, String reason) {
        String key = key(jobId);
        redisService.storeHash(key, FIELD_FAILURE_REASON, reason, ttlSeconds);
        redisService.storeHash(key, FIELD_STATUS, PendingOrdersStatus.FAILED.name(), ttlSeconds);
        log.warn("Submission {} marked FAILED: {}", jobId, reason);
    }
}
