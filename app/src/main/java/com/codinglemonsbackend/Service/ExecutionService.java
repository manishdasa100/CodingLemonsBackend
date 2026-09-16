package com.codinglemonsbackend.Service;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Payloads.SubmissionType;

/**
 * One code executor. Implementations own everything vendor-specific: how a job is handed over,
 * and how that vendor's raw result is normalized into {@link ExecutionReportDto}.
 *
 * Both executors converge on the same execution-results stream, so post-processing (persisting,
 * scoring, streaks) is written once in {@link ExecutionResultProcessor} and knows nothing about
 * which executor ran the code.
 */
public interface ExecutionService {

    ExecutorWorkerType getWorkerType();

    /**
     * Hands the submission over for execution. The job id is assigned by the caller and is
     * already set on {@code submissionMetadata}; implementations must carry it through so the
     * result can be correlated back.
     *
     * @throws RuntimeException if the executor did not accept the job - the caller may then
     *                          fall back to another executor.
     */
    void dispatch(SubmissionMetadata submissionMetadata);

    /** Normalizes this executor's raw report into the shape the rest of the app speaks. */
    ExecutionReportDto parseReport(String rawReport);

    /** Whether this executor is currently fit to take traffic. */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Feeds every completed report back to the executor that produced it, so health can be
     * judged on what the executor actually returns - a worker that is alive but failing every
     * job internally looks healthy to a liveness check.
     */
    default void recordOutcome(ExecutionReportDto report) {
        // no-op by default
    }

    /** Whether this executor can run the given submission type at all. */
    default boolean supports(SubmissionType submissionType) {
        return true;
    }
}
