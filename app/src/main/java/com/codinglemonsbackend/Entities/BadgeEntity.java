package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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

    @Transient
    public static final String ENTITY_COLLECTION_NAME = "Badges";

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String imageId;

    @Indexed(unique = true)
    private BadgeRule rule;
}
