package com.codinglemonsbackend.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Payloads.SubmissionType;

import lombok.extern.slf4j.Slf4j;

/**
 * Decides which executor runs a submission, and maps a worker type back to its executor when a
 * result comes home.
 *
 * Routing is deliberately an if-statement rather than a circuit breaker: enqueueing to the
 * NSJAIL worker's stream succeeds even when nothing is consuming it, so call failures are the
 * wrong signal. The executors report their own fitness instead - a heartbeat the worker
 * refreshes while it consumes, plus the run-of-internal-errors counter.
 */
@Slf4j
@Component
public class ExecutorRouter {

    /** Set this key to a worker type name to pin traffic during planned maintenance. */
    public static final String OVERRIDE_KEY = "executor:override";

    /** In-house executor first; Judge0 exists to catch traffic when it cannot. */
    private static final List<ExecutorWorkerType> PREFERENCE =
            List.of(ExecutorWorkerType.NSJAIL_WORKER, ExecutorWorkerType.JUDGE0_WORKER);

    private final Map<ExecutorWorkerType, ExecutionService> executors;
    private final RedisService redisService;

    public ExecutorRouter(List<ExecutionService> executionServices, RedisService redisService) {
        this.executors = executionServices.stream()
                .collect(Collectors.toMap(ExecutionService::getWorkerType, Function.identity()));
        this.redisService = redisService;
    }

    public ExecutionService get(ExecutorWorkerType workerType) {
        ExecutionService service = executors.get(workerType);
        if (service == null) {
            throw new IllegalArgumentException("No ExecutionService registered for worker type: " + workerType);
        }
        return service;
    }

    /**
     * Executors that can take this submission right now, best first. Empty means the submission
     * cannot run at all and the caller should fail fast rather than queue it nowhere.
     */
    public List<ExecutionService> candidates(SubmissionType submissionType) {
        ExecutorWorkerType pinned = readOverride();
        List<ExecutorWorkerType> order = pinned == null ? PREFERENCE : List.of(pinned);

        List<ExecutionService> candidates = new ArrayList<>(order.size());
        for (ExecutorWorkerType workerType : order) {
            ExecutionService service = executors.get(workerType);
            if (service == null) continue;
            if (!service.supports(submissionType)) continue;
            if (!service.isAvailable()) continue;
            candidates.add(service);
        }
        candidates.forEach(e -> System.out.println(e.getWorkerType() + " is a candidate for submission type " + submissionType));
        return candidates;
    }

    private ExecutorWorkerType readOverride() {
        String override = redisService.getValue(OVERRIDE_KEY);
        if (override == null || override.isBlank()) return null;
        try {
            return ExecutorWorkerType.valueOf(override.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring unrecognised executor override '{}'", override);
            return null;
        }
    }
}
