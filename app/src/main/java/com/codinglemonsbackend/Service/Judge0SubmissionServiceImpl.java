package com.codinglemonsbackend.Service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Config.SQSConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import com.codinglemonsbackend.Dto.ProblemExecutionDetails;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.Submission;
import com.codinglemonsbackend.Entities.TestcaseRegistry.TestcasePair;
import com.codinglemonsbackend.Repository.DriverCodeRepositoryService;
import com.codinglemonsbackend.Repository.SubmissionRepository;
import com.codinglemonsbackend.Repository.TestcaseRepositoryService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private DriverCodeRepositoryService driverCodeRepositoryService;

    @Autowired
    private TestcaseRepositoryService testcaseRepositoryService;

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

        private Boolean isRunCode;

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
    public String submitCode(SubmissionMetadata submissionMetadata) {

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

        Boolean isRunCode = submissionMetadata.getIsRunCode();

        ProgrammingLanguage programmingLanguage = submissionMetadata.getLanguage();

        Integer languageId = programmingLanguage.getLanguagId();

        ProblemExecutionDetails executionDetails = submissionMetadata.getExecutionDetails();

        String driverCode = driverCodeRepositoryService.getRegistry(problemId)
                            .get().getDriverCodes().get(programmingLanguage);

        // Driver code for the given programming language may not be present or null

        System.out.println("Driver code: " + driverCode);

        List<TestcasePair> testCases = testcaseRepositoryService.getRegistry(problemId)
                                        .get().getTestcases();

        testCases.stream().forEach(e -> System.out.println("Input:" + e.getInput() + " , " + "output: "+ e.getExpectedOutput()));

        String userCode = submissionMetadata.getUserCode();

        String sourceCode = SourceCodeFormatter.formatCode(driverCode, userCode, programmingLanguage);

        System.out.println("SOURCE CODE: ");
        System.out.println(sourceCode);

        String encodedSourceCode = Base64.getEncoder().encodeToString(sourceCode.getBytes());

        Float cpuTimeLimit = executionDetails.getCpuTimeLimit();

        Float memoryLimit = executionDetails.getMemoryLimit();

        Integer stackLimit = executionDetails.getStackLimit();

        // Float cpuTimeLimit = problemDto.getCpuTimeLimit();

        // Float memoryLimit = problemDto.getMemoryLimit();

        // Integer stackLimit = problemDto.getStackLimit();

        List<Judge0SubmissionRequestPayload> submissions = new ArrayList<Judge0SubmissionRequestPayload>();

        testCases.stream().limit((isRunCode)?runCodeTestCaseCount:testCases.size()).forEach((entry) -> {
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
            isRunCode, 
            submissions
        );
        
    }
    
}
