package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "ProblemOfTheDay")
public class ProblemOfTheDayMetadata {
    
    @Transient
    public static final String ENTITY_NAME = "ProblemOfTheDay";

    @Id
    private String id;

    private Integer problemId;
}
