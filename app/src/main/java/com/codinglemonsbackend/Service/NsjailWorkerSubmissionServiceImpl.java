package com.codinglemonsbackend.Service;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.ProblemExecutionDetails;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.TestcaseResult;
import com.codinglemonsbackend.Dto.TestcaseStatus;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;
import com.codinglemonsbackend.Repository.DriverCodeRepositoryService;
import com.codinglemonsbackend.Repository.SubmissionRepository;
import com.codinglemonsbackend.Repository.TestcaseRepositoryService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.extern.slf4j.Slf4j;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
public class NsjailWorkerSubmissionServiceImpl extends SubmissionService {

    private final DriverCodeRepositoryService driverCodeRepositoryService;

    private final TestcaseRepositoryService testcaseRepositoryService;

    private final SqsClient sqsClient;

    private final ObjectMapper objectMapper;

    private final Integer runCodeTestCaseCount;

    private final String pendingSubmissionsQueueUrl;

    @Autowired
    public NsjailWorkerSubmissionServiceImpl(
        SubmissionRepository submissionRepository,
        ModelMapper modelMapper,
        DriverCodeRepositoryService driverCodeRepositoryService,
        TestcaseRepositoryService testcaseRepositoryService,
        SqsClient sqsClient,
        ObjectMapper objectMapper,
        @Value("${testcase.runcode.count}") Integer runCodeTestCaseCount,
        @Value("${aws.sqs.queue.pending-submissions}") String pendingSubmissionsQueueUrl
    ) {
        super(submissionRepository, modelMapper);
        this.driverCodeRepositoryService = driverCodeRepositoryService;
        this.testcaseRepositoryService = testcaseRepositoryService;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.runCodeTestCaseCount = runCodeTestCaseCount;
        this.pendingSubmissionsQueueUrl = pendingSubmissionsQueueUrl;
    }

    private record SubmissionJob(
        String jobId,
        String language,
        String userCode,
        String driverCode,
        String task,
        int timeLimit,
        int memoryLimit,
        List<TestcasePair> testCases) {}

    @Override
    public String queueSubmission(SubmissionMetadata submissionMetadata) {
        
        if (submissionMetadata == null) {
            throw new IllegalArgumentException("Submission metadata cannot be null");
        }

        String submissionJobId = UUID.randomUUID().toString();
        submissionMetadata.setSubmissionJobId(submissionJobId);

        try {
            SubmissionJob submissionJob = createSubmissionJob(submissionMetadata);
            String messageBody = objectMapper.writeValueAsString(submissionJob);

            // Create hash based on content that should trigger deduplication (excluding jobId)
            String hashInput = submissionMetadata.getUsername() +
                    submissionMetadata.getProblemId() +
                    submissionMetadata.getUserCode() +
                    submissionMetadata.getLanguage() +
                    submissionMetadata.getIsRunCode();
            String messageDeduplicationId = generateHash(hashInput);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(pendingSubmissionsQueueUrl)
                    .messageBody(messageBody)
                    .messageGroupId(submissionMetadata.getUsername())
                    .messageDeduplicationId(messageDeduplicationId)
                    .build();

            sqsClient.sendMessage(sendMessageRequest);
            log.info("Successfully sent submission {} to SQS queue", submissionJobId);
        } catch (Exception e) {
            log.error("Failed to send submission {} to queue", submissionJobId, e);
            throw new RuntimeException("Failed to send submission to queue", e);
        }

        return submissionJobId;
    }

    private SubmissionJob createSubmissionJob(SubmissionMetadata submissionMetadata) {
        String jobId = submissionMetadata.getSubmissionJobId();
        Integer problemId = submissionMetadata.getProblemId();
        Boolean isRunCode = submissionMetadata.getIsRunCode();
        ProgrammingLanguage programmingLanguage = submissionMetadata.getLanguage();
        ProblemExecutionDetails executionDetails = submissionMetadata.getExecutionDetails();

        String driverCode = driverCodeRepositoryService.getRegistry(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Driver code registry not found for problem ID: " + problemId))
                .getDriverCodes()
                .get(programmingLanguage);

        List<TestcasePair> testCases = testcaseRepositoryService.getRegistry(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Test case registry not found for problem ID: " + problemId))
                .getTestcases();

        List<TestcasePair> testCasesToRun = isRunCode
                ? testCases.stream().limit(runCodeTestCaseCount).toList()
                : testCases;

        String userCode = submissionMetadata.getUserCode();
        if (!submissionMetadata.getB64Encoded()) {
            userCode = Base64.getEncoder().encodeToString(userCode.getBytes());
        }

        Float cpuTimeLimit = executionDetails.getCpuTimeLimit();
        Float memoryLimit = executionDetails.getMemoryLimit();

        return new SubmissionJob(
            jobId,
            programmingLanguage.name(),
            userCode,
            driverCode,
            isRunCode ? "RUN_CODE" : "SUBMIT_CODE",
            Math.round(cpuTimeLimit),
            Math.round(memoryLimit),
            testCasesToRun
        );
    }

    private String generateHash(String input) throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
    }

    // -------------------------------------------------------------------------
    // Raw response POJOs - field names must match what the NSJAIL worker writes
    // to Redis. Adjust if your worker uses different field names.
    // -------------------------------------------------------------------------

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private record NsjailTestcaseResult(
        int index,
        String status,          // PASSED(10), FAILED(20), TIMEOUT(30), MEMORY_EXCEED(40), OUTPUT_LIMIT(50), RUNTIME_ERROR(60), ERROR(70)
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
        String statusCode,          //  ACC(10), WA(20), TLE(30), MLE(40), OLE(50), CE(60), RE(70), IE(80)
        String statusMsg,
        int totalTestcases,
        int totalCorrect,
        int runtimeMs,
        int memoryMb,
        List<NsjailTestcaseResult> testResults,
        NsjailTestcaseResult failedTestcase,  // Optional - can be null if all passed(only applicable for SUBMIT_CODE task)
        String compileError,
        String runtimeError
    ) {}

    @Override
    public ExecutionReportDto constructExecutionReport(String report) {
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
                    .executionId(raw.executionId)
                    .language(raw.language)
                    .task(raw.task)
                    .status(status)
                    .statusMsg(raw.statusMsg)
                    .totalTestcases(raw.totalTestcases)
                    .totalCorrect(raw.totalCorrect)
                    .runtimeMs(raw.runtimeMs())
                    .memoryMb(raw.memoryMb())
                    .testcaseResults(testcaseResults)
                    .failedTestcase(failedTestcase)
                    .compileError(raw.compileError())
                    .runtimeError(raw.runtimeError())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse NSJAIL worker execution report", e);
        }
    }

    @Override
    public ExecutorWorkerType getWorkerType() {
        return ExecutorWorkerType.NSJAIL_WORKER;
    }
}
