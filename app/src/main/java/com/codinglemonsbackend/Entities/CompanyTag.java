package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "CompanyTags")
public class CompanyTag {

    @Id
    private String id;

    @NotEmpty
    private String name;

    @Indexed(unique = true)
    private String slug;

    @Pattern(regexp = "^https:\\/\\/(www\\.)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,10}(\\/[a-zA-Z0-9#?&%=._-]*)*$")
    private String websiteLink;

}
