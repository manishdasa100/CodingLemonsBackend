package com.codinglemonsbackend.Dto;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.codinglemonsbackend.Entities.CompanyTag;
import com.codinglemonsbackend.Entities.TopicTag;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
//@JsonIgnoreProperties(value={"testCases", "testCaseOutputs", "driverCodes", "cpuTimeLimit", "memoryLimit", "stackLimit"}, allowSetters = true)
// @JsonDeserialize(using=ProblemEntityDeserializer.class) 
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_NULL)
public class ProblemDto implements Serializable {

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

    private ProblemStatus status;
}
