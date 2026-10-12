package com.codinglemonsbackend.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionResultEnvelope;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Payloads.SubmissionType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class SubmissionMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void aFallbackIsCountedAgainstTheExecutorThatActuallyTookTheJob() throws Exception {
        ExecutionService nsjail = executor(ExecutorWorkerType.NSJAIL_WORKER);
        ExecutionService judge0 = executor(ExecutorWorkerType.JUDGE0_WORKER);
        doThrow(new RuntimeException("stream unavailable")).when(nsjail).dispatch(any());
        ExecutorRouter router = mock(ExecutorRouter.class);
        when(router.candidates(SubmissionType.RUN_CODE)).thenReturn(List.of(nsjail, judge0));
        RedisService redis = mock(RedisService.class);
        when(redis.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);

        new SubmissionDispatcher(router, mock(SubmissionJobStore.class), redis, registry, 300)
                .submit(SubmissionMetadata.builder().username("ada").problemId(7)
                        .userCode("print(1)").submissionType(SubmissionType.RUN_CODE).build());

        assertEquals(1.0, registry.get("submissions.dispatched")
                .tags("executor", "JUDGE0_WORKER", "task", "RUN_CODE").counter().count());
        assertEquals(0.0, registry.get("submissions.dispatched")
                .tags("executor", "NSJAIL_WORKER", "task", "RUN_CODE").counter().count(),
                "the executor that refused the job must not be credited with it");
    }

    @Test
    void aVerdictIsCountedOnceEvenWhenItsResultIsRedelivered() {
        SubmissionJobStore jobStore = mock(SubmissionJobStore.class);
        when(jobStore.exists("job-1")).thenReturn(true);
        when(jobStore.getMetadata("job-1")).thenReturn(SubmissionMetadata.builder()
                .submissionJobId("job-1").submissionType(SubmissionType.RUN_CODE).build());
        when(jobStore.ageSeconds("job-1")).thenReturn(3L);
        when(jobStore.claimForProcessing("job-1")).thenReturn(true, false);   // the redelivery finds it claimed

        ExecutionService nsjail = executor(ExecutorWorkerType.NSJAIL_WORKER);
        when(nsjail.parseReport("raw-report"))
                .thenReturn(ExecutionReportDto.builder().status(ExecutionStatus.ACC).build());
        ExecutorRouter router = mock(ExecutorRouter.class);
        when(router.get(ExecutorWorkerType.NSJAIL_WORKER)).thenReturn(nsjail);

        ExecutionResultProcessor processor = new ExecutionResultProcessor(router, jobStore,
                mock(SubmissionService.class), mock(UserSubmissionStatusService.class),
                mock(ApplicationEventPublisher.class), new ObjectMapper(), registry);
        ExecutionResultEnvelope result = new ExecutionResultEnvelope("job-1", "COMPLETED", "NSJAIL_WORKER", "raw-report");

        processor.process(result);
        processor.process(result);

        assertEquals(1.0, registry.get("submissions.completed")
                .tags("executor", "NSJAIL_WORKER", "task", "RUN_CODE", "status", "ACC").counter().count());
        assertEquals(1, registry.get("submissions.turnaround").timer().count());
    }

    private static ExecutionService executor(ExecutorWorkerType type) {
        ExecutionService executor = mock(ExecutionService.class);
        when(executor.getWorkerType()).thenReturn(type);
        return executor;
    } 
}
