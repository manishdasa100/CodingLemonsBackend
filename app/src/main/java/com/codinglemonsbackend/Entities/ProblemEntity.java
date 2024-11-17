package com.codinglemonsbackend.Entities;

import java.util.LinkedHashMap;
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
    private Integer id;
    private String title;
    private String description;
    private Set<String> constraints;
    private Set<Example> examples;
    private LinkedHashMap<String, String> testCasesWithExpectedOutputs;
    private Difficulty difficulty;
    private Map<ProgrammingLanguage, String> driverCodes;
    private Map<ProgrammingLanguage, String> codeSnippets;
    private float cpuTimeLimit;
    private float memoryLimit;
    private Integer stackLimit;
    private Set<TopicTag> topics;
    private Set<CompanyTag> companies;
    private Integer previousProblemId;
    private Integer nextProblemId;
    private Integer acceptedCount;
    private Integer submissionCount;
}
