package com.codinglemonsbackend.Entities;

import java.util.List;

import org.springframework.data.annotation.Id;
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
    
    @Id
    private String id;
    
    private String name;

    private String description;

    private List<Integer> problemIds;

    @Indexed
    private String creator;

    private List<ProblemEntity> problemsData;
}
