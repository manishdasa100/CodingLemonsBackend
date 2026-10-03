package com.codinglemonsbackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.TestcaseResult;
import com.codinglemonsbackend.Dto.TestcaseStatus;
import com.codinglemonsbackend.Payloads.SubmissionType;
import com.codinglemonsbackend.Service.ExecutionService;
import com.codinglemonsbackend.Service.ExecutorRouter;
import com.codinglemonsbackend.Service.Judge0ExecutionServiceImpl;
import com.codinglemonsbackend.Service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Covers the two places the dual-executor pipeline can break silently: the normalized report
 * failing to survive its trip through Redis, and routing not falling back when the in-house
 * worker is down.
 */
class ExecutionPipelineTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The result processor stores the normalized report and the poll endpoint reads it back. If
     * records stop round-tripping, every submission completes into an unreadable result.
     */
    @Test
    void executionReportSurvivesTheRedisRoundTrip() throws Exception {
        ExecutionReportDto original = ExecutionReportDto.builder()
                .executionId("job-1")
                .language("JAVA")
                .task("SUBMIT_CODE")
                .status(ExecutionStatus.WA)
                .statusMsg("Wrong Answer")
                .totalTestcases(3)
                .totalCorrect(2)
                .runtimeMs(120)
                .memoryMb(16)
                .testcaseResults(List.of(TestcaseResult.builder()
                        .index(0)
                        .status(TestcaseStatus.PASSED)
                        .input("1 2")
                        .expectedOutput("3")
                        .actualOutput("3")
                        .runtimeMs(40)
                        .memoryMb(8)
                        .build()))
                .failedTestcase(TestcaseResult.builder()
                        .index(2)
                        .status(TestcaseStatus.FAILED)
                        .input("4 5")
                        .expectedOutput("9")
                        .actualOutput("8")
                        .build())
                .build();

        ExecutionReportDto restored = objectMapper.readValue(
                objectMapper.writeValueAsString(original), ExecutionReportDto.class);

        assertEquals(original, restored);
        assertEquals(ExecutionStatus.WA, restored.status());
        assertEquals(2, restored.failedTestcase().index());
    }

    /** Judge0's per-testcase verdicts have to collapse into the same overall shape NSJAIL produces. */
    @Test
    void judge0ReportNormalizesToTheSharedShape() {
        Judge0ExecutionServiceImpl judge0 = newJudge0Service();

        String rawReport = """
            {"jobId":"job-2","language":"PYTHON","task":"SUBMIT_CODE","testcaseResults":[
              {"index":0,"statusId":3,"statusDescription":"Accepted","stdout":"3","time":"0.012","memory":3072,
               "input":"1 2","expectedOutput":"3"},
              {"index":1,"statusId":4,"statusDescription":"Wrong Answer","stdout":"8","time":"0.020","memory":4096,
               "input":"4 5","expectedOutput":"9"}
            ]}""";

        ExecutionReportDto report = judge0.parseReport(rawReport);

        assertEquals("job-2", report.executionId());
        assertEquals(ExecutionStatus.WA, report.status());
        assertEquals(2, report.totalTestcases());
        assertEquals(1, report.totalCorrect());
        assertEquals(20, report.runtimeMs());      // slowest testcase, seconds converted to ms
        assertEquals(4, report.memoryMb());        // hungriest testcase, KB converted to MB
        assertEquals(1, report.failedTestcase().index());
        assertNull(report.compileError());
    }

    /**
     * NSJAIL omits per-testcase detail for SUBMIT_CODE and reports only the first failure, because
     * a submission runs the full hidden judge set. Judge0 reports every testcase identically for
     * both request types, so parseReport has to draw that line - otherwise a Judge0 submission
     * hands the client every hidden input and expected output, and the two executors disagree.
     */
    @Test
    void judge0WithholdsTestcaseDetailForSubmitCodeOnly() {
        Judge0ExecutionServiceImpl judge0 = newJudge0Service();

        String rawReport = """
            {"jobId":"job-4","language":"PYTHON","task":"%s","testcaseResults":[
              {"index":0,"statusId":3,"statusDescription":"Accepted","stdout":"3","time":"0.012","memory":3072,
               "input":"1 2","expectedOutput":"3"},
              {"index":1,"statusId":4,"statusDescription":"Wrong Answer","stdout":"8","time":"0.020","memory":4096,
               "input":"hidden 4 5","expectedOutput":"9"}
            ]}""";

        ExecutionReportDto submitted = judge0.parseReport(rawReport.formatted("SUBMIT_CODE"));

        assertTrue(submitted.testcaseResults().isEmpty(), "SUBMIT_CODE must not disclose per-testcase results");
        assertNotNull(submitted.failedTestcase(), "SUBMIT_CODE discloses the first failure instead");
        assertEquals("hidden 4 5", submitted.failedTestcase().input());
        // Withholding the list must not corrupt the aggregates, which are still taken from it.
        assertEquals(2, submitted.totalTestcases());
        assertEquals(1, submitted.totalCorrect());
        assertEquals(20, submitted.runtimeMs());
        assertEquals(4, submitted.memoryMb());

        ExecutionReportDto ran = judge0.parseReport(rawReport.formatted("RUN_CODE"));

        assertEquals(2, ran.testcaseResults().size(), "RUN_CODE runs only visible testcases, so detail is kept");
        assertEquals("hidden 4 5", ran.testcaseResults().get(1).input());
        assertNull(ran.failedTestcase(), "RUN_CODE reports failures in the list, not separately");
    }

    /** A compile error fails every testcase, so it has to win over the per-testcase verdicts. */
    @Test
    void judge0CompileErrorBecomesTheOverallStatus() {
        Judge0ExecutionServiceImpl judge0 = newJudge0Service();

        String rawReport = """
            {"jobId":"job-3","language":"JAVA","task":"RUN_CODE","testcaseResults":[
              {"index":0,"statusId":6,"statusDescription":"Compilation Error","compileOutput":"missing semicolon"}
            ]}""";

        ExecutionReportDto report = judge0.parseReport(rawReport);

        assertEquals(ExecutionStatus.CE, report.status());
        assertEquals("missing semicolon", report.compileError());
        assertEquals(0, report.totalCorrect());
    }

    @Test
    void routerPrefersNsjailButFallsBackToJudge0() {
        RedisService redisService = mock(RedisService.class);
        when(redisService.getValue(ExecutorRouter.OVERRIDE_KEY)).thenReturn(null);

        FakeExecutor nsjail = new FakeExecutor(ExecutorWorkerType.NSJAIL_WORKER, true, true);
        FakeExecutor judge0 = new FakeExecutor(ExecutorWorkerType.JUDGE0_WORKER, true, true);
        ExecutorRouter router = new ExecutorRouter(List.of(nsjail, judge0), redisService);

        List<ExecutionService> healthy = router.candidates(SubmissionType.SUBMIT_CODE);
        assertEquals(ExecutorWorkerType.NSJAIL_WORKER, healthy.get(0).getWorkerType());
        assertEquals(2, healthy.size());

        nsjail.available = false;
        List<ExecutionService> degraded = router.candidates(SubmissionType.SUBMIT_CODE);
        assertEquals(1, degraded.size());
        assertEquals(ExecutorWorkerType.JUDGE0_WORKER, degraded.get(0).getWorkerType());

        // Nowhere to send it: intake must fail fast rather than queue it into the void.
        judge0.available = false;
        assertTrue(router.candidates(SubmissionType.SUBMIT_CODE).isEmpty());
    }

    @Test
    void routerHonoursTheOverrideKeyAndExecutorCapabilities() {
        RedisService redisService = mock(RedisService.class);
        FakeExecutor nsjail = new FakeExecutor(ExecutorWorkerType.NSJAIL_WORKER, true, true);
        FakeExecutor judge0 = new FakeExecutor(ExecutorWorkerType.JUDGE0_WORKER, true, false);
        ExecutorRouter router = new ExecutorRouter(List.of(nsjail, judge0), redisService);

        // Judge0 cannot calibrate, so it must not be offered even while it is healthy.
        when(redisService.getValue(ExecutorRouter.OVERRIDE_KEY)).thenReturn(null);
        List<ExecutionService> forCalibration = router.candidates(SubmissionType.CALIBRATE);
        assertEquals(1, forCalibration.size());
        assertEquals(ExecutorWorkerType.NSJAIL_WORKER, forCalibration.get(0).getWorkerType());

        // Pinning traffic by hand wins over the preference order.
        when(redisService.getValue(ExecutorRouter.OVERRIDE_KEY)).thenReturn("JUDGE0_WORKER");
        List<ExecutionService> pinned = router.candidates(SubmissionType.SUBMIT_CODE);
        assertEquals(1, pinned.size());
        assertEquals(ExecutorWorkerType.JUDGE0_WORKER, pinned.get(0).getWorkerType());

        assertNotNull(router.get(ExecutorWorkerType.NSJAIL_WORKER));
    }

    private Judge0ExecutionServiceImpl newJudge0Service() {
        // Only parseReport is exercised here, so the collaborators it never reaches stay null.
        return new Judge0ExecutionServiceImpl(
                RestClient.builder(), objectMapper, null, null, null, null,
                2, "stream:execution-results", "", "", "", 20, 300L);
    }

    private static final class FakeExecutor implements ExecutionService {
        private final ExecutorWorkerType workerType;
        private boolean available;
        private final boolean supportsCalibration;

        FakeExecutor(ExecutorWorkerType workerType, boolean available, boolean supportsCalibration) {
            this.workerType = workerType;
            this.available = available;
            this.supportsCalibration = supportsCalibration;
        }

        @Override public ExecutorWorkerType getWorkerType() { return workerType; }
        @Override public boolean isAvailable() { return available; }
        @Override public boolean supports(SubmissionType submissionType) {
            return supportsCalibration || submissionType != SubmissionType.CALIBRATE;
        }
        @Override public void dispatch(SubmissionMetadata submissionMetadata) { }
        @Override public ExecutionReportDto parseReport(String rawReport) { return null; }
    }
}
