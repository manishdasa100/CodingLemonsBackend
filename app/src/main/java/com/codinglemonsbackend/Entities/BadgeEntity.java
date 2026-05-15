package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.codinglemonsbackend.Dto.BadgeRule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "Badges")
public class BadgeEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String imageId;

    private BadgeRule rule;
}
