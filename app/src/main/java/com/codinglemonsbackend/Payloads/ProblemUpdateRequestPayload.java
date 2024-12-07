package com.codinglemonsbackend.Payloads;

import java.util.Map;
import java.util.Set;
import java.util.List;

import com.codinglemonsbackend.Dto.Difficulty;
import com.codinglemonsbackend.Dto.Example;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemUpdateRequestPayload {

    private String title;

    private String description;

    private Set<String> constraints;

    private Set<Example> examples;

    private Map<String, String> testCasesWithExpectedOutputs;

    private List<String> testCaseOutputs;

    private Map<ProgrammingLanguage, String> driverCodes;

    private Difficulty difficulty;

    private Float cpuTimeLimit;

    private Float memoryLimit;

    private Integer stackLimit;

    private Set<String> topics;

    private Set<String> companyTags;
}
