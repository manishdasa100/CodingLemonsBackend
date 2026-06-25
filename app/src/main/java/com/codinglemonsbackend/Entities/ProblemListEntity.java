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

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "ProblemList")
@CompoundIndex(name = "unique_name_creator",def = "{'creator':1, 'name':1}", unique = true)
public class ProblemListEntity {

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "ProblemList";
    
    @Id
    private String id;
    
    private String name;

    private String description;

    private Set<Integer> problemIds = new HashSet<>();

    private Boolean isPublic;
    
    private Boolean isPinned;

    private Boolean isStudyPlan;

    private StudyPlanDifficultyTier difficultyTier;

    private Integer timelineDays;

    private String creator;

    public void setProblemIds(Set<Integer> problemIds) {
        this.problemIds = problemIds == null ? new HashSet<>() : problemIds;
    }
}
