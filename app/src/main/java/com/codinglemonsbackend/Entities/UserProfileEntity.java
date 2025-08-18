package com.codinglemonsbackend.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "UserProfiles")
public class UserProfileEntity {
    
    @Id
    private String username;

    private String firstName;

    private String lastName;

    private String email;

    private String profilePictureId;

    private String githubUrl;

    private String twitterUrl;

    private String linkedinUrl;

    private Integer score;

    private String rank;

    private String avatarUrl;

    private String about;

    private String school;

    private String city;

    private String country;

    private String companySlug;

    private String jobTitle;

    private SkillTags[] skillTags;
}
