package com.codinglemonsbackend.Entities;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.Difficulty;
import com.codinglemonsbackend.Dto.Example;
import com.codinglemonsbackend.Dto.ProgrammingLanguage;

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
    private Set<String> constraints;
    private Set<Example> examples;
    private Difficulty difficulty;
    private Map<ProgrammingLanguage, String> codeSnippets;
    private Set<TopicTag> topics;
    private Set<CompanyTag> companies;
    private Integer likes;
    private Integer previousProblemId;
    private Integer nextProblemId;
    private Integer acceptedCount;
    private Integer submissionCount;
}
