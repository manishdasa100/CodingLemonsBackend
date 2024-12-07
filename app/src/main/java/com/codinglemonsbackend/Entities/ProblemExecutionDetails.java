package com.codinglemonsbackend.Entities;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.ProgrammingLanguage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "ProblemExecutionDetails")
@Builder
public class ProblemExecutionDetails {

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "ProblemExecutionDetails";
    
    @Id
    private Integer id;
    private float cpuTimeLimit;
    private float memoryLimit;
    private Integer stackLimit;
    private Map<ProgrammingLanguage, String> driverCodes;
    private LinkedHashMap<String, String> testCasesWithExpectedOutputs;
}
