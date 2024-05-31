package com.codinglemonsbackend.Entities;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.Example;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "problems")
@Builder
public class ProblemEntity {

    @Transient
    public static final String SEQUENCE_NAME = "problem_sequence";
    
    @Id
    Integer problemId;
    String title;
    String description;
    Set<String> constraints;
    Set<Example> examples;
    Map<String, String> testCasesWithExpectedOutputs;
    List<String> testCaseOutputs;
    Difficulty difficulty;
    Map<ProgrammingLanguage, String> driverCodes;
    float cpuTimeLimit;
    float memoryLimit;
    Integer stackLimit;
    //Map<ProgrammingLanguage, String> optimalSolutions;
    Set<String> topics;
    Set<String> companyTags;
    Integer acceptance;
    Integer previousProblemId;
    Integer nextProblemId;
}
