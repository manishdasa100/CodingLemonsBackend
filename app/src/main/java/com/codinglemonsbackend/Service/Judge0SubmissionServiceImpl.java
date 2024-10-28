package com.codinglemonsbackend.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codinglemonsbackend.Config.RabbitMQConfig;
import com.codinglemonsbackend.Dto.ProblemDto;
import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.ProgrammingLanguage;
import com.codinglemonsbackend.Entities.Submission;
import com.codinglemonsbackend.Repository.SubmissionRepository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Service
public class Judge0SubmissionServiceImpl implements SubmissionService{

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ModelMapper modelMapper;

    private final Integer runCodeTestCaseCount = 2;

    @Override
    public SubmissionDto getSubmission(String submissionId) {
        
        Optional<Submission> submissionEntity = submissionRepository.getSubmission(submissionId);

        if (submissionEntity.isEmpty()) throw new NoSuchElementException("No submission found for id "+submissionId);

        SubmissionDto submissionDto = modelMapper.map(submissionEntity.get(), SubmissionDto.class);
        
        return submissionDto;
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

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, createSubmissionJob(submissionMetadata));
        
        return submissionJobId;
    }

    private SubmissionJob createSubmissionJob(SubmissionMetadata submissionMetadata) {

        System.out.println("RECEIVED SUBMISSION FROM " + submissionMetadata.getUsername());

        Boolean isRunCode = submissionMetadata.getIsRunCode();

        String submissionJobId = submissionMetadata.getSubmissionJobId();

        ProgrammingLanguage programmingLanguage = submissionMetadata.getLanguage();

        Integer languageId = programmingLanguage.getLanguagId();

        ProblemDto problemDto = submissionMetadata.getProblemDto();

        Map<String, String> testCases = problemDto.getTestCasesWithExpectedOutputs();

        String driverCode = problemDto.getDriverCodes().get(programmingLanguage);

        String userCode = submissionMetadata.getUserCode();

        String sourceCode = SourceCodeFormatter.formatCode(driverCode, userCode, programmingLanguage);

        Float cpuTimeLimit = problemDto.getCpuTimeLimit();

        Float memoryLimit = problemDto.getMemoryLimit();

        Integer stackLimit = problemDto.getStackLimit();

        List<Judge0SubmissionRequestPayload> submissions = new ArrayList<Judge0SubmissionRequestPayload>();

        testCases.entrySet().stream().limit((isRunCode)?runCodeTestCaseCount:testCases.size()).forEach((entry) -> {
            System.out.println("test case : "+ entry.getKey());
            Judge0SubmissionRequestPayload payload = Judge0SubmissionRequestPayload.builder()
            .source_code(sourceCode)
            .language_id(languageId)
            .stdin(entry.getKey())
            .expected_output(entry.getValue())
            .cpu_time_limit(cpuTimeLimit)
            .memory_limit(memoryLimit)
            .stack_limit(stackLimit)
            .enable_network(false)
            .build();

            submissions.add(payload);
        });
   
        // for (int i = 0; i < testCases.size(); i++) {
        //     Judge0SubmissionRequestPayload payload = Judge0SubmissionRequestPayload.builder()
        //     .source_code(Base64.getEncoder().encodeToString(sourceCode.getBytes()))
        //     .language_id(languageId)
        //     .stdin(Base64.getEncoder().encodeToString(testCases.get(i)..getBytes()))
        //     .expected_output(Base64.getEncoder().encodeToString(testCaseOutputs.get(i).getBytes()))
        //     .cpu_time_limit(cpuTimeLimit)
        //     .memory_limit(memoryLimit)
        //     .stack_limit(stackLimit)
        //     .enable_network(false)
        //     .build();

        //     submissions.add(payload);
        // }

        return new SubmissionJob(
            submissionJobId, 
            submissionMetadata.getUsername(), 
            problemDto.getProblemId(), 
            problemDto.getDifficulty().getPoints(),
            isRunCode, 
            submissions
        );
        
    }
    
}
