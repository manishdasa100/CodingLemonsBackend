package com.codinglemonsbackend.Entities;

import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
@CompoundIndex(name = "unique_name_creator",def = "{'creator':1, 'name':1}", unique = true)
public class ProblemListEntity {

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "UserProblemList";
    
    @Id
    private String id;
    
    private String name;

    private String description;

    private Set<Integer> problemIds;

    private Boolean isPublic;
    
    private Boolean isPinned;

    private String creator;
}
