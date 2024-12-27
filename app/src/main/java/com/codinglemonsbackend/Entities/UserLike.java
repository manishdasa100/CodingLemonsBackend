package com.codinglemonsbackend.Entities;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "UserLikes")
public class UserLike {

    private String id;
    
    private Integer problemId;

    private String userId;

    private LocalDateTime createdAt;
}
