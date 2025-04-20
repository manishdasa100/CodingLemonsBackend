package com.codinglemonsbackend.Service;

import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Dto.SubmissionMetadata;

public interface SubmissionService {
    
    // public Mono<Judge0CreateSubmissionResponse> createSubmission(SubmissionMetadata submissionDto);

    // public Mono<List<Judge0SubmissionToken>> submitCode(SubmissionMetadata submissionDto);

    public <T> T submitCode(SubmissionMetadata submissionDto);

    public SubmissionDto getSubmission(String submissionId);
}
