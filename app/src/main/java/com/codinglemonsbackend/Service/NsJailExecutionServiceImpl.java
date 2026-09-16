package com.codinglemonsbackend.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.SupportedLanguage;
import com.codinglemonsbackend.Dto.TestcaseResult;
import com.codinglemonsbackend.Dto.TestcaseStatus;
import com.codinglemonsbackend.Entities.ProblemExecutionLimits;
import com.codinglemonsbackend.Entities.TestcaseRegistry;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;
import com.codinglemonsbackend.Payloads.SubmissionType;
import com.codinglemonsbackend.Repository.DriverCodeRepository;
import com.codinglemonsbackend.Repository.TestcaseRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.extern.slf4j.Slf4j;

/**
 * In-house executor: jobs go onto a Redis stream that the NSJAIL worker consumes, results come
 * back on the execution-results stream. Nothing here is synchronous - the stream is also what
 * makes the worker crash-safe and independently scalable.
 */
@Service
@Slf4j
public class NsJailExecutionServiceImpl implements ExecutionService {

    /** Refreshed by the worker inside its consume loop; absence means the loop is not turning. */
    public static final String HEARTBEAT_KEY = "executor:nsjail:heartbeat";

    /** Consecutive internal errors - a worker that is up but broken trips this instead. */
    public static final String CONSECUTIVE_IE_KEY = "executor:nsjail:consecutive-ie";

    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final DriverCodeRepository driverCodeRepository;
    private final TestcaseRepository testcaseRepository;
    private final Integer runCodeTestCaseCount;
    private final String pendingSubmissionsStream;
    private final boolean requireHeartbeat;
    private final int maxConsecutiveIe;
    private final long ieCooldownSeconds;

    public NsJailExecutionServiceImpl(
            DriverCodeRepository driverCodeRepository,
            TestcaseRepository testcaseRepository,
            ObjectMapper objectMapper,
            RedisService redisService,
            @Value("${testcase.runcode.count}") Integer runCodeTestCaseCount,
            @Value("${queue.pending-submissions.stream}") String pendingSubmissionsStream,
            // Defaults off: the worker does not publish a heartbeat yet, and defaulting on would
            // park the only real executor on any environment that has not set this explicitly.
            @Value("${executor.nsjail.require-heartbeat:false}") boolean requireHeartbeat,
            @Value("${executor.nsjail.max-consecutive-ie:5}") int maxConsecutiveIe,
            @Value("${executor.nsjail.ie-cooldown-seconds:600}") long ieCooldownSeconds) {
        this.driverCodeRepository = driverCodeRepository;
        this.testcaseRepository = testcaseRepository;
        this.objectMapper = objectMapper;
        this.redisService = redisService;
        this.runCodeTestCaseCount = runCodeTestCaseCount;
        this.pendingSubmissionsStream = pendingSubmissionsStream;
        this.requireHeartbeat = requireHeartbeat;
        this.maxConsecutiveIe = maxConsecutiveIe;
        this.ieCooldownSeconds = ieCooldownSeconds;
    }

    @Override
    public ExecutorWorkerType getWorkerType() {
        return ExecutorWorkerType.NSJAIL_WORKER;
    }

    @Override
    public boolean isAvailable() {
        if (requireHeartbeat && !Boolean.TRUE.equals(redisService.keyExist(HEARTBEAT_KEY))) {
            log.warn("NSJAIL worker heartbeat missing - executor considered unavailable");
            return false;
        }
        String consecutiveIe = redisService.getValue(CONSECUTIVE_IE_KEY);
        if (consecutiveIe != null && Integer.parseInt(consecutiveIe) >= maxConsecutiveIe) {
            log.warn("NSJAIL worker returned {} consecutive internal errors - executor considered unavailable",
                    consecutiveIe);
            return false;
        }
        return true;
    }

    /**
     * A run of internal errors means the worker is up but cannot execute anything. The counter
     * carries a TTL so the executor is retried automatically once the run stops.
     */
    @Override
    public void recordOutcome(ExecutionReportDto report) {
        if (report != null && report.status() == ExecutionStatus.IE) {
            redisService.incrementWithTtl(CONSECUTIVE_IE_KEY, 1, ieCooldownSeconds);
        } else {
            redisService.deleteKey(CONSECUTIVE_IE_KEY);
        }
    }

    @Override
    public void dispatch(SubmissionMetadata submissionMetadata) {
        String messageBody;
        try {
            messageBody = objectMapper.writeValueAsString(createSubmissionJob(submissionMetadata));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare submission for the NSJAIL queue", e);
        }

        redisService.addToStream(pendingSubmissionsStream, Map.of("body", messageBody));
        log.info("Queued submission {} to stream {}", submissionMetadata.getSubmissionJobId(), pendingSubmissionsStream);
    }

    private record SubmissionJob(
        String jobId,
        String language,
        String userCode,
        String slowCode,
        String hogCode,
        String driverCode,
        String task,
        Integer timeLimit,
        Integer memoryLimit,
        List<TestcasePair> testCases) {}

    private SubmissionJob createSubmissionJob(SubmissionMetadata submissionMetadata) {
        String jobId = submissionMetadata.getSubmissionJobId();
        Integer problemId = submissionMetadata.getProblemId();
        SubmissionType submissionType = submissionMetadata.getSubmissionType();
        SupportedLanguage programmingLanguage = submissionMetadata.getLanguage();
        ProblemExecutionLimits executionDetails = submissionMetadata.getExecutionLimits();

        String driverCode = driverCodeRepository.getByProblemId(problemId)
                .orElseThrow(() -> new IllegalStateException("Driver code registry not found for problem ID: " + problemId))
                .getDriverCodes()
                .get(programmingLanguage);

        List<TestcasePair> testCases = this.getTargetTestcases(submissionType, problemId);

        boolean alreadyEncoded = Boolean.TRUE.equals(submissionMetadata.getB64Encoded());

        String userCode = encode(submissionMetadata.getUserCode(), alreadyEncoded);

        Integer cpuTimeLimit = (executionDetails != null) ? executionDetails.getCpuTimeLimit() : null;
        Integer memoryLimit = (executionDetails != null) ? executionDetails.getMemoryLimit() : null;

        String slowCode = null, hogCode = null;

        if (submissionType == SubmissionType.CALIBRATE) {
            slowCode = encode(submissionMetadata.getSlowCode(), alreadyEncoded);
            hogCode = encode(submissionMetadata.getHogCode(), alreadyEncoded);
        }

        return new SubmissionJob (
            jobId,
            programmingLanguage.name(),
            userCode,
            slowCode,
            hogCode,
            driverCode,
            submissionType.name(),
            cpuTimeLimit,
            memoryLimit,
            testCases
        );
    }

    private String encode(String userCode, boolean alreadyEncoded) {
        return (userCode == null || alreadyEncoded) ? userCode : Base64.getEncoder().encodeToString(userCode.getBytes());
    }

    private List<TestcasePair> getTargetTestcases(SubmissionType submissionType, Integer problemId) {
        TestcaseRegistry registry = testcaseRepository.getByProblemId(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Test case registry not found for problem ID: " + problemId));
        List<TestcasePair> testcases = switch (submissionType) {
            case CALIBRATE -> registry.getCalibrationTestcases();
            case SUBMIT_CODE, TRIAL_RUN -> registry.getJudgeTestcases();
            case RUN_CODE -> registry.getJudgeTestcases().stream().limit(runCodeTestCaseCount).toList();
            default -> throw new IllegalArgumentException("Unexpected value: " + submissionType);
        };
        if (testcases == null || testcases.isEmpty()) {
            throw new IllegalStateException("No " + (submissionType == SubmissionType.CALIBRATE ? "calibration" : "judge")
                + " testcases configured for problem " + problemId);
        }

        return testcases;
    }

    // -------------------------------------------------------------------------
    // Raw report shape - field names must match what the NSJAIL worker writes.
    // -------------------------------------------------------------------------

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record NsjailTestcaseResult(
        int index,
        String status,          // PASSED, FAILED, TIMEOUT, MEMORY_EXCEED, OUTPUT_LIMIT, RUNTIME_ERROR, ERROR
        String inputData,
        String expectedOutput,
        String actualOutput,
        String stdOutput,
        String stderr,
        Integer runtimeMs,
        Integer memoryMb,
        String errorMessage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record NsjailExecutionReport(
        String executionId,
        String language,
        String task,
        String statusCode,          //  ACC, WA, TLE, MLE, OLE, CE, RE, IE
        String statusMsg,
        int totalTestcases,
        int totalCorrect,
        int runtimeMs,
        int memoryMb,
        List<NsjailTestcaseResult> testResults,
        NsjailTestcaseResult failedTestcase,  // null when everything passed (SUBMIT_CODE only)
        String compileError,
        String runtimeError,
        String internalError,
        Map<String, Object> calibration
    ) {}

    @Override
    public ExecutionReportDto parseReport(String report) {
        try {
            NsjailExecutionReport raw = objectMapper.readValue(report, NsjailExecutionReport.class);

            ExecutionStatus status = ExecutionStatus.valueOf(raw.statusCode());

            List<TestcaseResult> testcaseResults = raw.testResults() == null
                    ? List.of()
                    : raw.testResults().stream()
                            .map(r -> new TestcaseResult(
                                r.index(),
                                TestcaseStatus.valueOf(r.status()),
                                r.inputData(),
                                r.expectedOutput(),
                                r.actualOutput(),
                                r.stdOutput(),
                                r.stderr(),
                                r.runtimeMs(),
                                r.memoryMb(),
                                r.errorMessage()
                            ))
                            .toList();

            NsjailTestcaseResult failedTestcaseResult = raw.failedTestcase();

            TestcaseResult failedTestcase = failedTestcaseResult == null
                    ? null
                    : TestcaseResult.builder()
                            .index(failedTestcaseResult.index())
                            .status(TestcaseStatus.valueOf(failedTestcaseResult.status()))
                            .input(failedTestcaseResult.inputData())
                            .expectedOutput(failedTestcaseResult.expectedOutput())
                            .actualOutput(failedTestcaseResult.actualOutput())
                            .stdOut(failedTestcaseResult.stdOutput())
                            .stdErr(failedTestcaseResult.stderr())
                            .runtimeMs(failedTestcaseResult.runtimeMs())
                            .memoryMb(failedTestcaseResult.memoryMb())
                            .errorMsg(failedTestcaseResult.errorMessage())
                            .build();

            return ExecutionReportDto.builder()
                    .executionId(raw.executionId())
                    .language(raw.language())
                    .task(raw.task())
                    .status(status)
                    .statusMsg(raw.statusMsg())
                    .totalTestcases(raw.totalTestcases())
                    .totalCorrect(raw.totalCorrect())
                    .runtimeMs(raw.runtimeMs())
                    .memoryMb(raw.memoryMb())
                    .testcaseResults(testcaseResults)
                    .failedTestcase(failedTestcase)
                    .compileError(raw.compileError())
                    .runtimeError(raw.runtimeError())
                    .internalError(raw.internalError())
                    .calibrationReport(raw.calibration())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse NSJAIL worker execution report", e);
        }
    }
}
