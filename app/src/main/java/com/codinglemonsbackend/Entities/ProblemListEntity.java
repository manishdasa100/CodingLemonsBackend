package com.codinglemonsbackend.Entities;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "UserProblemList")
public class ProblemListEntity {

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "UserProblemList";
    
    @Id
    private String id;
    
    private String name;

    private String description;

    private List<Integer> problemIds;

    private Boolean isPinned;

    @Indexed
    private String creator;
}
