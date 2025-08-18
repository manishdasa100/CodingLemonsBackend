package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "Company")
public class Company {

    public static final String ENTITY_COLLECTION_NAME = "Company";

    @Id
    private String id;

    private String name;

    private String slug;

    private String websiteLink;

    private String companyLogoId;

}
