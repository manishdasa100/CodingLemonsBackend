package com.codinglemonsbackend.Entities;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "UserStudyPlanProgress")
@CompoundIndex(def = "{'listId':1, 'owner':1}")
@Data
public class UserStudyPlanProgress {

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "UserStudyPlanProgress";
    
    @Id
    private String id;

    private String listId;

    private String owner;

    private Set<Integer> solvedProblemIds = new HashSet<>();

    public UserStudyPlanProgress(String listId, String owner) {
        this.listId = listId;
        this.owner = owner;
    }
}
