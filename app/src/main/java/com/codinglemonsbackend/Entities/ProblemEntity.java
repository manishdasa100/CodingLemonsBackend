package com.codinglemonsbackend.Entities;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.ProblemDto.Example;
import com.codinglemonsbackend.Dto.ProblemDto.Difficulty;
import com.codinglemonsbackend.Dto.ProblemStatus;
import com.codinglemonsbackend.Dto.SupportedLanguage;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Problems")
@Builder
public class ProblemEntity {

    @Transient
    public static final String SEQUENCE_NAME = "problem_sequence";

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "Problems";
    
    @Id
    private Integer id;
    private String title;
    private String description;
    private List<String> constraints;
    private List<Example> examples;
    private Difficulty difficulty;
    private Map<SupportedLanguage, String> codeSnippets;
    private Set<String> topics;
    private Set<String> companies;
    private ProblemExecutionLimits executionLimits;
    private Integer likes;
    private Integer previousProblemId;
    private Integer nextProblemId;
    private Integer acceptedCount;
    private Integer submissionCount;
    private ProblemStatus status;
}
