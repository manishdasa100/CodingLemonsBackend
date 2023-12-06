package com.codinglemonsbackend.Payloads;

import java.util.List;

import com.codinglemonsbackend.Dto.ProblemDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemSetResponse {
    
    private Long total;

    private List<ProblemDto> problems;
}
