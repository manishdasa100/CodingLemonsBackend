package com.codinglemonsbackend.Entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "UserLikes")
@CompoundIndex(name = "unique_like_username_problemId", def = "{'problemId':1, 'username':1}", unique = true)
@ToString
public class UserLike {

    @Id
    private String id;
    
    private Integer problemId;

    private String username;

    private LocalDateTime createdAt;

    public UserLike(Integer problemId, String username, LocalDateTime createdAt) {
        this.problemId = problemId;
        this.username = username;
        this.createdAt = createdAt;
    }
}
