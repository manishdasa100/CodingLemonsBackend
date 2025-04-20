package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Document(collection = "database_sequences")
public class DatabaseSequence {

    public static final String ENTITY_COLLECTION_NAME = "database_sequences";
    
    @Id
    private String id;

    private Integer seq;
}
