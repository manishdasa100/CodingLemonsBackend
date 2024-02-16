package com.codinglemonsbackend.Payloads;

import java.util.List;

import com.codinglemonsbackend.Dto.ProblemDtoWithStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemSetResponsePayload {
    
    private Long total;

    private List<ProblemDtoWithStatus> problems;
}
