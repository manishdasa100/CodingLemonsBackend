package com.codinglemonsbackend.Entities;

import java.util.List;
import java.util.Map;

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
    List<String> constraints;
    List<Example> examples;
    List<String> testCases;
    List<String> testCaseOutputs;
    Difficulty difficulty;
    Map<ProgrammingLanguage, String> driverCodes;
    Map<ProgrammingLanguage, String> optimalSolutions;
    List<String> topics;
    Integer acceptance;
}
