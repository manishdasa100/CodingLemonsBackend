package com.codinglemonsbackend.Payloads;

import com.codinglemonsbackend.Dto.SubmissionDto;
import com.codinglemonsbackend.Entities.Submission;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(value = Include.NON_NULL)
public class SubmissionResponsePayload<T> {

    private String submissionStatus;

    private T submission;
}
