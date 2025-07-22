package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "Topic")
public class Topic {

    public static final String ENTITY_COLLECTION_NAME = "Topic";
    
    @Id
    private String id;

    @NotEmpty
    private String name;

    @Indexed(unique = true)
    private String slug;

}
