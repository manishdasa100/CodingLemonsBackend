package com.codinglemonsbackend.Service;

import com.codinglemonsbackend.Payloads.CodeSubmissionResponsePayload;

public interface SubmissionService {
    
    public String createSubmission();

    public CodeSubmissionResponsePayload getSubmission(String submissionId);
}
