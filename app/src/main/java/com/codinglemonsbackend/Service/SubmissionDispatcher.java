package com.codinglemonsbackend.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Exceptions.DuplicateSubmissionException;
import com.codinglemonsbackend.Exceptions.FailedSubmissionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Submission intake: assigns the job id the frontend polls with, rejects repeats, records the
 * job before handing it anywhere, and falls back to the next executor if the chosen one will
 * not take it.
 *
 * The job id is ours in every case - Judge0's own tokens stay an internal detail of
 * {@link Judge0ExecutionServiceImpl} - so the frontend cannot tell which executor ran the code.
 */
@Slf4j
@Service
public class SubmissionDispatcher {

    /**
     * The only thing a user is told when a submission cannot be placed. Which executor was tried,
     * which vendor it was and why it objected are operator concerns, and leaking them hands out
     * a map of the execution backend for free.
     */
    private static final String SUBMISSION_FAILED_MESSAGE =
            "We could not run your submission right now. Please try again in a few minutes.";

    private final ExecutorRouter executorRouter;
    private final SubmissionJobStore jobStore;
    private final RedisService redisService;
    private final long dedupTtlSeconds;

    public SubmissionDispatcher(
            ExecutorRouter executorRouter,
            SubmissionJobStore jobStore,
            RedisService redisService,
            @Value("${executor.dedup.ttl-seconds:300}") long dedupTtlSeconds) {
        this.executorRouter = executorRouter;
        this.jobStore = jobStore;
        this.redisService = redisService;
        this.dedupTtlSeconds = dedupTtlSeconds;
    }

    public String submit(SubmissionMetadata submissionMetadata) throws FailedSubmissionException {
        if (submissionMetadata == null) {
            throw new IllegalArgumentException("Submission metadata cannot be null");
        }

        String jobId = UUID.randomUUID().toString();
        submissionMetadata.setSubmissionJobId(jobId);

        List<ExecutionService> candidates = executorRouter.candidates(submissionMetadata.getSubmissionType());
        if (candidates.isEmpty()) {
            log.error("No executor can take a {} submission right now", submissionMetadata.getSubmissionType());
            throw new FailedSubmissionException(SUBMISSION_FAILED_MESSAGE);
        }

        String dedupKey = RedisService.SUBMISSION_DEDUP_KEY + deduplicationId(submissionMetadata);
        if (!Boolean.TRUE.equals(redisService.setIfAbsent(dedupKey, jobId, dedupTtlSeconds))) {
            throw new DuplicateSubmissionException("A duplicate submission found. Please wait before resubmitting.");
        }

        try {
            return dispatchToFirstAccepting(submissionMetadata, candidates);
        } catch (Exception e) {
            // Nothing was accepted, so nothing is in flight: clear the way for an immediate retry.
            jobStore.delete(jobId);
            redisService.deleteKey(dedupKey);
            throw e;
        }
    }

    private String dispatchToFirstAccepting(SubmissionMetadata metadata, List<ExecutionService> candidates)
            throws FailedSubmissionException {

        String jobId = metadata.getSubmissionJobId();
        RuntimeException lastFailure = null;

        for (ExecutionService executor : candidates) {
            // Written before dispatch so a result can never arrive before the job exists.
            jobStore.create(jobId, metadata, executor.getWorkerType());
            try {
                executor.dispatch(metadata);
                return jobId;
            } catch (RuntimeException e) {
                lastFailure = e;
                log.error("Executor {} refused submission {}", executor.getWorkerType(), jobId, e);
            }
        }

        // The cause is already logged per executor above. It names the executor, its vendor and
        // its validation errors, none of which is the user's business - so it stops here.
        log.error("No executor accepted submission {} - last failure: {}", jobId,
                lastFailure == null ? "none" : lastFailure.toString());
        throw new FailedSubmissionException(SUBMISSION_FAILED_MESSAGE);
    }

    /**
     * Identifies a repeat of the same code by the same user - deliberately excluding the job id,
     * which is new every time.
     */
    private String deduplicationId(SubmissionMetadata metadata) {
        String input = metadata.getUsername()
                + metadata.getProblemId()
                + metadata.getUserCode()
                + metadata.getLanguage()
                + metadata.getSubmissionType();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
