package com.codinglemonsbackend.Payloads;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Judge0SubmissionResponsePayload extends CodeSubmissionResponsePayload{

    @Data
    @Builder
    public static class Status{
        private Integer id;
        private String description;
    }
    
    private String sourceCode;

    private Integer languageId;

    private Status status;

    private String createdAt;

    private String finishedAt;

}
