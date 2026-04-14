package com.codinglemonsbackend.Service;

import com.codinglemonsbackend.Dto.ProblemExecutionDetails;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;
import com.codinglemonsbackend.Repository.DriverCodeRepositoryService;
import com.codinglemonsbackend.Repository.SubmissionRepository;
import com.codinglemonsbackend.Repository.TestcaseRepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkerSubmissionServiceImpl extends SubmissionService {

    private final DriverCodeRepositoryService driverCodeRepositoryService;

    private final TestcaseRepositoryService testcaseRepositoryService;

    private final SqsClient sqsClient;

    private final ObjectMapper objectMapper;

    private final Integer runCodeTestCaseCount;

    private final String pendingSubmissionsQueueUrl;

    @Autowired
    public WorkerSubmissionServiceImpl(
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
    public String submitCode(SubmissionMetadata submissionMetadata) {
        
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


}
