package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(collection = "UserRank")
public class UserRank {
    
    @Id
    private String id;

    @Indexed(unique = true)
    private String rankName;

    private Integer milestonePoints;

    private String rankBadgeId;
}
