package com.codinglemonsbackend.Dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LikeEvent {
    
    private Integer problemId;

    private String username;

    private LocalDateTime createdAt;

    private Boolean isLike;
}
