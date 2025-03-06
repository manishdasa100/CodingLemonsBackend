package com.codinglemonsbackend.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProblemMetadata {
    
    private String problemId;

    private String likes;

    private Boolean likeStatus;
}
