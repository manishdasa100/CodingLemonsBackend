package com.codinglemonsbackend.Payloads;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmitCodeRequestPayload {
    
    @NotEmpty
    private Integer problemId;

    @NotEmpty
    private ProgrammingLanguage language;

    @NotEmpty
    private String userCode;
}
