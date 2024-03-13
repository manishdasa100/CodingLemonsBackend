package com.codinglemonsbackend.Service;

import java.util.List;

import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;
import com.codinglemonsbackend.Entities.Submission;
import com.codinglemonsbackend.Payloads.CodeSubmissionResponsePayload;
import com.codinglemonsbackend.Service.Judge0SubmissionServiceImpl.Judge0SubmissionToken;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SubmissionService {
    
    // public Mono<Judge0CreateSubmissionResponse> createSubmission(SubmissionMetadata submissionDto);

    // public Mono<List<Judge0SubmissionToken>> submitCode(SubmissionMetadata submissionDto);

    public <T> T submitCode(SubmissionMetadata submissionDto);

    public SubmissionDto getSubmission(String submissionId);

    public String saveSubmission(Submission submission);
}
