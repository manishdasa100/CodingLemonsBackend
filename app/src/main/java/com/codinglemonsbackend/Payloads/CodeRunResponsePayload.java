package com.codinglemonsbackend.Payloads;

import com.codinglemonsbackend.Dto.CodeRunResultDto;
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
public class CodeRunResponsePayload {
    
    private String submissionStatus;

    private CodeRunResultDto codeRunResult;
}
