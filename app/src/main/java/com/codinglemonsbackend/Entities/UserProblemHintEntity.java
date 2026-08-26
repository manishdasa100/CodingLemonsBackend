package com.codinglemonsbackend.Entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.ExecutionStatus;
import com.codinglemonsbackend.Dto.Hint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "UserProblemHints")
@CompoundIndex(def = "{'username':1, 'problemId':1}", unique = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProblemHintEntity {
    @Id
    private String id;
    private String username;
    private Integer problemId;
    private Map<ExecutionStatus, List<Hint>> hints;

    public UserProblemHintEntity(String username, Integer problemId) {
        this.username = username;
        this.problemId = problemId;
        this.hints = new HashMap<>();
    }
}
