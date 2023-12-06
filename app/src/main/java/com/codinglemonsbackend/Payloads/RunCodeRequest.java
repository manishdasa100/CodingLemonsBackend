package com.codinglemonsbackend.Payloads;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunCodeRequest {

    private Integer problemId;

    private ProgrammingLanguage language;

    private String userCode;
    
}
