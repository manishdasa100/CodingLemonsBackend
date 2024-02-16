package com.codinglemonsbackend.Payloads;

import com.codinglemonsbackend.Entities.ProgrammingLanguage;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunCodeRequestPayload {

    @NotEmpty
    private Integer problemId;

    @NotNull
    private ProgrammingLanguage language;

    @NotEmpty
    private String userCode;

    @NotEmpty
    private String dataInput;
}
