package com.codinglemonsbackend.Service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionResultEnvelope;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Events.SubmitCodeCompletedEvent;
import com.codinglemonsbackend.Payloads.SubmissionType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Everything that happens once a submission finishes, for either executor: normalize the report,
 * persist it, move the problem between solved and attempted, and announce the completion.
 *
 * Runs on the results-stream consumer thread rather than inside the poll endpoint, so a user who
 * closes the tab still gets scored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionResultProcessor {

    private final ExecutorRouter executorRouter;
    private final SubmissionJobStore jobStore;
    private final SubmissionService submissionService;
    private final UserSubmissionStatusService userSubmissionStatusService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * @throws RuntimeException when the failure looks transient, so the caller leaves the stream
     *                          entry unacknowledged and it gets redelivered. Malformed payloads
     *                          fail the job instead - retrying them would never succeed.
     */
    public void process(ExecutionResultEnvelope envelope) {
        String jobId = envelope.jobId();
        if (jobId == null) {
            log.warn("Execution result carried no job id - discarding it");
            return;
        }

        if (!jobStore.exists(jobId)) {
            log.warn("Execution result for unknown or expired job {} - discarding it", jobId);
            return;
        }

        ExecutorWorkerType workerType = resolveWorkerType(envelope, jobId);
        if (workerType == null) {
            jobStore.markFailed(jobId, "Execution result did not identify its executor");
            return;
        }

        if (isFailure(envelope)) {
            jobStore.markFailed(jobId, "Executor " + workerType + " reported status " + envelope.status());
            return;
        }

        ExecutionService executor = executorRouter.get(workerType);

        ExecutionReportDto report;
        String normalizedReportJson;
        try {
            report = executor.parseReport(envelope.executionReport());
            normalizedReportJson = objectMapper.writeValueAsString(report);
        } catch (Exception e) {
            // Deterministic: the same bytes will not parse on a retry either.
            log.error("Could not read the {} report for job {}", workerType, jobId, e);
            jobStore.markFailed(jobId, "Execution result could not be read");
            return;
        }

        executor.recordOutcome(report);

        SubmissionMetadata metadata = jobStore.getMetadata(jobId);
        if (metadata == null) {
            jobStore.markFailed(jobId, "Submission metadata is no longer available");
            return;
        }

        // Claiming before the side effects is what stops a redelivered result from scoring twice.
        if (!jobStore.claimForProcessing(jobId)) {
            log.info("Job {} was already processed - ignoring the duplicate result", jobId);
            return;
        }

        try {
            if (metadata.getSubmissionType() == SubmissionType.SUBMIT_CODE) {
                recordSubmission(report, metadata);
            }
            jobStore.markCompleted(jobId, normalizedReportJson);
            log.info("Completed submission {} executed by {} with status {}", jobId, workerType, report.status());
        } catch (RuntimeException e) {
            // ponytail: releasing the claim makes a redelivery retryable; a crash between the
            // claim and this line still strands the job. Move to a claim with a lease if that
            // window ever bites.
            jobStore.releaseClaim(jobId);
            throw e;
        }
    }

    private void recordSubmission(ExecutionReportDto report, SubmissionMetadata metadata) {
        submissionService.saveSubmission(report, metadata);

        boolean isNewSolve = false;
        if (report.status() == ExecutionStatus.ACC) {
            isNewSolve = userSubmissionStatusService.addToSolvedAndRemoveFromAttempted(metadata);
        } else {
            userSubmissionStatusService.addToAttemptedIfNotSolved(metadata);
        }

        eventPublisher.publishEvent(new SubmitCodeCompletedEvent(this, report, metadata, isNewSolve));
    }

    private ExecutorWorkerType resolveWorkerType(ExecutionResultEnvelope envelope, String jobId) {
        if (envelope.workerType() != null) {
            try {
                return ExecutorWorkerType.valueOf(envelope.workerType());
            } catch (IllegalArgumentException e) {
                log.warn("Execution result for job {} named an unknown executor '{}'", jobId, envelope.workerType());
            }
        }
        // Fall back to whichever executor intake handed the job to.
        return jobStore.getWorkerType(jobId);
    }

    private boolean isFailure(ExecutionResultEnvelope envelope) {
        String status = envelope.status();
        if (envelope.executionReport() == null) return true;
        return status != null && status.equalsIgnoreCase(PendingOrdersStatus.FAILED.name());
    }
}
