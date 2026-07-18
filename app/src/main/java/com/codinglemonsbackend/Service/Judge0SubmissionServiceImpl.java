package com.codinglemonsbackend.Service;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import com.codinglemonsbackend.Dto.ExecutionReportDto;
import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.ExecutorWorkerType;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Dto.TestcaseResult;
import com.codinglemonsbackend.Dto.TestcaseStatus;
import com.codinglemonsbackend.Entities.ProblemExecutionLimits;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;
import com.codinglemonsbackend.Payloads.SubmissionType;
import com.codinglemonsbackend.Repository.DriverCodeRepository;
import com.codinglemonsbackend.Repository.SubmissionRepository;
import com.codinglemonsbackend.Repository.TestcaseRepository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Service
public class Judge0SubmissionServiceImpl extends SubmissionService{

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${aws.sqs.queue.pending-submissions}")
    private String pendingSubmissionsQueueUrl;

    @Autowired
    private DriverCodeRepository driverCodeRepository;

    @Autowired
    private TestcaseRepository testcaseRepository;

    private final Integer runCodeTestCaseCount = 2;

    @Autowired
    public Judge0SubmissionServiceImpl(SubmissionRepository submissionRepository, ModelMapper modelMapper) {
        super(submissionRepository, modelMapper);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Judge0SubmissionRequestPayload{
        private String source_code;
        private int language_id;
        private String stdin;
        private String expected_output;
        private float cpu_time_limit;
        private float memory_limit;
        private int stack_limit;
        private boolean enable_network;
        private final boolean redirect_stderr_to_stdout = true;

        @Override
        public String toString(){
            return "{ " + source_code + ", " + language_id + ", " + stdin + ", " + expected_output + ", " + cpu_time_limit + ", " + memory_limit + ", " + stack_limit + ", " + enable_network + ", " + redirect_stderr_to_stdout + " }"; 
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private class SubmissionJob{

        private String submissionJobId;

        private String username;

        private Integer problemId;

        private Integer solutionPoints;

        private SubmissionType submissionType;

        private List<Judge0SubmissionRequestPayload> submissions;

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Judge0SubmissionToken{

        private String token;
    }

    /*@Override
    public Mono<List<Judge0SubmissionToken>> submitCode(SubmissionMetadata submissionMetadata) {

        Judge0BulkSubmissionPayload bulkSubmissionPayload = getPayloadData(submissionMetadata);

        Mono<List<Judge0SubmissionToken>> res = webclient.post()
                .uri(uriBuilder -> uriBuilder.queryParam("base64_encoded", "true").build())
                .header("X-RapidAPI-Key", "2b9918893cmshdf8db6d467e8893p1dfbd5jsn0a4ee838a160")
                .header("X-RapidAPI-Host", "judge0-ce.p.rapidapi.com")
                .bodyValue(bulkSubmissionPayload)
                .retrieve().bodyToMono(new ParameterizedTypeReference<List<Judge0SubmissionToken>>(){});
                                
        return res;
    }*/
    @Override
    public String queueSubmission(SubmissionMetadata submissionMetadata) {

        System.out.println("RECEIVED SUBMISSION FROM " + submissionMetadata.getUsername());

        String submissionJobId = UUID.randomUUID().toString();

        submissionMetadata.setSubmissionJobId(submissionJobId);

        try {
            SubmissionJob submissionJob = createSubmissionJob(submissionMetadata);
            String messageBody = objectMapper.writeValueAsString(submissionJob);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(pendingSubmissionsQueueUrl)
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(sendMessageRequest);
        } catch (Exception e) {
            System.err.println("Error sending message to SQS: " + e.getMessage());
            throw new RuntimeException("Failed to send submission to queue", e);
        }

        return submissionJobId;
    }

    private SubmissionJob createSubmissionJob(SubmissionMetadata submissionMetadata) {
        
        String submissionJobId = submissionMetadata.getSubmissionJobId();

        Integer problemId = submissionMetadata.getProblemId();

        SubmissionType submissionType = submissionMetadata.getSubmissionType();

        ProgrammingLanguage programmingLanguage = submissionMetadata.getLanguage();

        Integer languageId = programmingLanguage.getLanguagId();

        ProblemExecutionLimits executionDetails = submissionMetadata.getExecutionLimits();

        String driverCode = driverCodeRepository.getByProblemId(problemId)
                            .orElseThrow(() -> new IllegalArgumentException("Driver code registry not found for problem ID: " + problemId))
                            .getDriverCodes().get(programmingLanguage);

        // Driver code for the given programming language may not be present or null

        System.out.println("Driver code: " + driverCode);

        List<TestcasePair> testCases = testcaseRepository.getByProblemId(problemId)
                                        .orElseThrow(() -> new IllegalArgumentException("Test case registry not found for problem ID: " + problemId))
                                        .getJudgeTestcases();

        testCases.stream().forEach(e -> System.out.println("Input:" + e.getInput() + " , " + "output: "+ e.getExpectedOutput()));

        String userCode = submissionMetadata.getUserCode();

        String sourceCode = SourceCodeFormatter.formatCode(driverCode, userCode, programmingLanguage);

        System.out.println("SOURCE CODE: ");
        System.out.println(sourceCode);

        String encodedSourceCode = Base64.getEncoder().encodeToString(sourceCode.getBytes());

        Integer cpuTimeLimit = executionDetails.getCpuTimeLimit();

        Integer memoryLimit = executionDetails.getMemoryLimit();

        Integer stackLimit = executionDetails.getStackLimit();

        // Float cpuTimeLimit = problemDto.getCpuTimeLimit();

        // Float memoryLimit = problemDto.getMemoryLimit();

        // Integer stackLimit = problemDto.getStackLimit();

        List<Judge0SubmissionRequestPayload> submissions = new ArrayList<Judge0SubmissionRequestPayload>();

        testCases.stream().limit((submissionType == SubmissionType.RUN_CODE) ? runCodeTestCaseCount : testCases.size()).forEach((entry) -> {
            System.out.println("test case : "+ entry.getInput());
            Judge0SubmissionRequestPayload payload = Judge0SubmissionRequestPayload.builder()
            .source_code(encodedSourceCode)
            .language_id(languageId)
            .stdin(Base64.getEncoder().encodeToString(entry.getInput().getBytes()))
            .expected_output(Base64.getEncoder().encodeToString(entry.getExpectedOutput().getBytes()))
            .cpu_time_limit(cpuTimeLimit)
            .memory_limit(memoryLimit)
            .stack_limit(stackLimit)
            .enable_network(false)
            .build();

            submissions.add(payload);
        });

        return new SubmissionJob(
            submissionJobId, 
            submissionMetadata.getUsername(), 
            submissionMetadata.getProblemId(), 
            submissionMetadata.getSolutionPoints(),
            submissionType, 
            submissions
        );
        
    }

    // -------------------------------------------------------------------------
    // Raw response POJOs - field names must match what the Judge0 worker writes
    // to Redis after aggregating all per-testcase Judge0 API responses.
    // -------------------------------------------------------------------------

    private record Judge0RawTestcaseResult(
        int index,
        int statusId,               // Judge0 status id (3=ACC, 4=WA, 5=TLE, 6=CE, 7-12=RE, 13+=IE)
        String statusDescription,
        String stdout,
        String stderr,
        String compile_output,
        String time,                // seconds as string, e.g. "0.123"
        Integer memory,             // KB
        String input,
        String expectedOutput
    ) {}

    private record Judge0ExecutionResult(
        String jobId,
        String language,
        String task,
        List<Judge0RawTestcaseResult> testcaseResults
    ) {}

    @Override
    public ExecutionReportDto constructExecutionReport(String report) {
        try {
            Judge0ExecutionResult raw = objectMapper.readValue(report, Judge0ExecutionResult.class);

            List<TestcaseResult> testcaseResults = raw.testcaseResults().stream()
                    .map(r -> new TestcaseResult(
                            r.index(),
                            mapTestcaseStatus(r.statusId()),
                            r.input(),
                            r.expectedOutput(),
                            r.stdout(),
                            r.stdout(),
                            r.stderr(),
                            r.time() != null ? (int) (Float.parseFloat(r.time()) * 1000) : null,
                            r.memory() != null ? r.memory() / 1024 : null,
                            r.statusDescription()
                    ))
                    .toList();

            // Compile error from any testcase (CE applies to all, so first is enough)
            String compileError = raw.testcaseResults().stream()
                    .map(Judge0RawTestcaseResult::compile_output)
                    .filter(c -> c != null && !c.isBlank())
                    .findFirst()
                    .orElse(null);

            // Overall status: compile error takes priority, then first non-ACC testcase
            ExecutionStatus overallStatus;
            if (compileError != null) {
                overallStatus = ExecutionStatus.CE;
            } else {
                overallStatus = raw.testcaseResults().stream()
                        .map(r -> mapOverallStatus(r.statusId()))
                        .filter(s -> s != ExecutionStatus.ACC)
                        .findFirst()
                        .orElse(ExecutionStatus.ACC);
            }

            long totalCorrect = testcaseResults.stream()
                    .filter(r -> r.status() == TestcaseStatus.PASSED)
                    .count();

            TestcaseResult firstFailed = testcaseResults.stream()
                    .filter(r -> r.status() != TestcaseStatus.PASSED)
                    .findFirst()
                    .orElse(null);

            TestcasePair failedTestcase = firstFailed != null
                    ? new TestcasePair(firstFailed.input(), firstFailed.expectedOutput())
                    : null;

            // return new ExecutionReportDto(
            //         raw.jobId(),
            //         raw.language(),
            //         raw.task(),
            //         testcaseResults.size(),
            //         (int) totalCorrect,
            //         overallStatus,
            //         overallStatus.getStatusMessage(),
            //         testcaseResults,
            //         failedTestcase,
            //         compileError,
            //         null,
            //         null,
            //         ExecutorWorkerType.JUDGE0_WORKER
            // );
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Judge0 execution report", e);
        }
    }

    @Override
    public ExecutorWorkerType getWorkerType() {
        return ExecutorWorkerType.JUDGE0_WORKER;
    }

    // Judge0 status IDs: 3=ACC, 4=WA, 5=TLE, 6=CE, 7-12=RE, 13+=IE
    private static ExecutionStatus mapOverallStatus(int statusId) {
        return switch (statusId) {
            case 3 -> ExecutionStatus.ACC;
            case 4 -> ExecutionStatus.WA;
            case 5 -> ExecutionStatus.TLE;
            case 6 -> ExecutionStatus.CE;
            case 7, 8, 9, 10, 11, 12 -> ExecutionStatus.RE;
            default -> ExecutionStatus.IE;
        };
    }

    private static TestcaseStatus mapTestcaseStatus(int statusId) {
        return switch (statusId) {
            case 3 -> TestcaseStatus.PASSED;
            case 4 -> TestcaseStatus.FAILED;
            case 5 -> TestcaseStatus.TIMEOUT;
            case 7, 8, 9, 10, 11, 12 -> TestcaseStatus.RUNTIME_ERROR;
            default -> TestcaseStatus.ERROR;
        };
    }
}
